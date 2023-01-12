package io.gingersnapproject.database;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.infinispan.commons.dataconversion.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gingersnapproject.configuration.Configuration;
import io.gingersnapproject.configuration.Rule;
import io.gingersnapproject.database.model.Table;
import io.gingersnapproject.database.vendor.Vendor;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;

@Singleton
public class DatabaseHandler {
   private static final Logger log = LoggerFactory.getLogger(DatabaseHandler.class);
   @Inject
   Configuration configuration;
   @Inject
   Pool pool;

   Vendor vendor;

   Map<String, Table> tables = Collections.emptyMap();
   Map<String, String> table2rule = Collections.emptyMap();

   void start(@Observes StartupEvent ignore, Config config) {
      String dbKind = config.getValue("quarkus.datasource.db-kind", String.class);
      vendor = Vendor.fromDbKind(dbKind);
      refreshSchema();
   }

   public void refreshSchema() {
      tables = configuration.rules().values().stream()
            .map(rule -> vendor.describeTable(pool, rule.connector().table()).await().indefinitely())
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(t -> t.name(), Function.identity()));
      table2rule = configuration.rules().entrySet().stream()
            .collect(Collectors.toMap(t -> t.getValue().connector().table(), t -> t.getKey()));
   }

   public Uni<String> select(String rule, String key) {
      Rule ruleConfig = configuration.rules().get(rule);
      if (ruleConfig == null) {
         throw new IllegalArgumentException("No rule found for " + rule);
      }

      // Have to do this until bug is fixed allowing injection of reactive Pool
      var query = prepareQuery(pool, ruleConfig.selectStatement());

      String[] arguments = ruleConfig.keyType().toArguments(key, ruleConfig.plainSeparator());
      return query
            .execute(Tuple.from(arguments))
            .onFailure().invoke(t -> log.error("Exception encountered!", t))
            .map(rs -> {
               if (rs.size() > 1) {
                  throw new IllegalArgumentException("Result set for " + ruleConfig.selectStatement() + " for key: " + key + " returned " + rs.size() + " rows, it should only return 1");
               }
               int columns = rs.columnsNames().size();
               RowIterator<Row> rowIterator = rs.iterator();
               if (!rowIterator.hasNext()) {
                  return null;
               }
               Row row = rowIterator.next();
               Json jsonObject = Json.object();
               for (int i = 0; i < columns; ++i) {
                  jsonObject.set(row.getColumnName(i), row.getValue(i));
               }
               return jsonObject.toString();
            });
   }

   public static PreparedQuery<RowSet<Row>> prepareQuery(Pool pool, String sql) {
      io.vertx.sqlclient.PreparedQuery<RowSet<Row>> preparedQuery = pool.preparedQuery(sql);
      return io.vertx.mutiny.sqlclient.PreparedQuery.newInstance(preparedQuery);
   }

   public Table table(String name) {
      return tables.get(name);
   }

   public String tableToRuleName(String name) {
      return table2rule.get(name);
   }
}
