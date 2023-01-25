package io.gingersnapproject.database.vendor;

import static io.gingersnapproject.database.DatabaseHandler.prepareQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gingersnapproject.database.model.Column;
import io.gingersnapproject.database.model.ForeignKey;
import io.gingersnapproject.database.model.JavaType;
import io.gingersnapproject.database.model.PrimaryKey;
import io.gingersnapproject.database.model.Table;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;

public class MySQLVendor extends AbstractVendor {
   private static final Logger log = LoggerFactory.getLogger(MySQLVendor.class);

   @Override
   public Uni<Table> describeTable(Pool pool, String table) {

      Uni<List<Column>> uni = prepareQuery(pool, "DESC " + table + ";")
            .execute()
            .onFailure().invoke(t -> log.error("Error retrieving table description for " + table, t))
            .map(rs -> {
               List<Column> columns = new ArrayList<>();
               // Column 1: Field - name of column
               // Column 2: Type - type of column
               // Column 3: Null - whether column is nullable
               // Column 4: Key - what kind of key constraint it may have (e.g primary or unique)
               // Column 5: Default - what the default values if ro the column
               // Column 6: Extra - additional properties such as auto_increment
               for (Row row : rs) {
                  String columnName = row.getString(0);
                  String columnType = row.getString(1);
                  columns.add(new Column(columnName, JavaType.fromString(columnType)));
               }
               return columns;
            });

      return uni.onItem().transformToUni(columns -> {
         Map<String, Column> columnMap = columns.stream().collect(Collectors.toMap(c -> c.name().toUpperCase(), Function.identity()));
         // Obtain the PK columns
         Uni<PrimaryKey> pkUni =
               prepareQuery(pool, "select constraint_name, column_name from information_schema.key_column_usage where table_name = ? and constraint_name in (select constraint_name from information_schema.table_constraints where table_name = ? and constraint_type='PRIMARY KEY') order by ordinal_position")
                     .execute(Tuple.of(table, table))
                     .onFailure().invoke(t -> log.error("Error retrieving primary key from " + table, t))
                     .map(rs -> {
                        String pkName = null;
                        List<Column> pkColumns = new ArrayList<>(2);
                        for (Row row : rs) {
                           pkName = row.getString(0);
                           String pkColumnName = row.getString(1).toUpperCase();
                           Column pkColumn = columnMap.get(pkColumnName);
                           if (pkColumn == null) {
                              throw new IllegalStateException("PK Column " + pkColumnName + " not found in columns: " + columns);
                           }
                           pkColumns.add(pkColumn);
                        }
                        return new PrimaryKey(pkName, pkColumns);
                     });
         Uni<List<ForeignKey>> fksUni =
               prepareQuery(pool, "select constraint_name, referenced_table_name, column_name, referenced_column_name from information_schema.key_column_usage where table_name = ? and constraint_name in (select constraint_name from information_schema.table_constraints\n" +
                     " where table_name = ? and constraint_type='FOREIGN KEY') order by constraint_name, ordinal_position")
                     .execute(Tuple.of(table, table))
                     .onFailure().invoke(t -> log.error("Error retrieving foreign keys from " + table, t))
                     .map(rs -> {
                        if (rs.size() == 0) {
                           return Collections.emptyList();
                        }
                        List<ForeignKey> foreignKeys = new ArrayList<>(2);
                        String fkName = null;
                        List<Column> fkColumns = new ArrayList<>(2);
                        String fkTable = null;
                        List<String> refFkColumns = new ArrayList<>(2);
                        for (Row row : rs) {
                           String newFkName = row.getString(0);
                           if (fkName != null && !newFkName.equals(fkName)) {
                              foreignKeys.add(new ForeignKey(fkName, fkColumns, fkTable, refFkColumns));
                              fkColumns = new ArrayList<>(2);
                              refFkColumns = new ArrayList<>(2);
                           }
                           fkName = newFkName;
                           fkTable = row.getString(1);

                           String fkColumnName = row.getString(2).toUpperCase();
                           Column pkColumn = columnMap.get(fkColumnName);
                           if (pkColumn == null) {
                              throw new IllegalStateException("FK Column " + fkColumnName + " not found in columns: " + columns);
                           }

                           fkColumns.add(pkColumn);
                           refFkColumns.add(row.getString(3));
                        }
                        foreignKeys.add(new ForeignKey(fkName, fkColumns, fkTable, refFkColumns));
                        return foreignKeys;
                     });
         return Uni.combine().all().unis(pkUni, fksUni)
               // TODO: add unique constraints
               .combinedWith((pk, fks) -> new Table(table, columns, pk, fks, null))
               .onFailure().call(t -> {
                  log.error("Error encountered while processing table columns", t);
                  return Uni.createFrom().nullItem();
               });
      });
   }
}
