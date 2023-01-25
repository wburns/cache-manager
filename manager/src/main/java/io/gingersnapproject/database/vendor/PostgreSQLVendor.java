package io.gingersnapproject.database.vendor;

import static io.gingersnapproject.database.DatabaseHandler.prepareQuery;

import java.util.ArrayList;
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

public class PostgreSQLVendor extends AbstractVendor {
   private static final Logger log = LoggerFactory.getLogger(PostgreSQLVendor.class);

   @Override
   public Uni<Table> describeTable(Pool pool, String table) {

      Uni<List<Column>> uni = prepareQuery(pool, """
            select column_name, data_type 
             from information_schema.columns 
            where table_name = '$1';""")
            .execute(Tuple.of(table))
            .onFailure().invoke(t -> log.error("Error retrieving table description for " + table, t))
            .map(rs -> {
               List<Column> columns = new ArrayList<>();
               for (Row row : rs) {
                  String columnName = row.getString(0);
                  String columnType = row.getString(1);
                  columns.add(new Column(columnName, JavaType.valueOf(columnType)));
               }
               return columns;
            });

      return uni.onItem().transformToUni(columns -> {
         Map<String, Column> columnMap = columns.stream().collect(Collectors.toMap(c -> c.name().toUpperCase(), Function.identity()));
         // Obtain the PK columns
         Uni<PrimaryKey> pkUni =
               prepareQuery(pool, "select constraint_name, column_name from information_schema.key_column_usage where table_name = '$1' and constraint_name in (select constraint_name from information_schema.table_constraints where table_name = '$2' and constraint_type='PRIMARY KEY') order by ordinal_position")
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
               prepareQuery(pool, "select kcu.constraint_name, kcu.column_name, ccu.table_name as ref_table, ccu.column_name as ref_column from information_schema.key_column_usage kcu, information_schema.constraint_column_usage ccu where kcu.constraint_name=ccu.constraint_name and kcu.table_name = '$1' and kcu.constraint_name in (select constraint_name from information_schema.table_constraints where table_name = '$2' and constraint_type='FOREIGN KEY') order by kcu.ordinal_position")
                     .execute(Tuple.of(table, table))
                     .onFailure().invoke(t -> log.error("Error retrieving foreign keys from " + table, t))
                     .map(rs -> {
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

                           String fkColumnName = row.getString(1).toUpperCase();
                           Column fkColumn = columnMap.get(fkColumnName);
                           if (fkColumn == null) {
                              throw new IllegalStateException("FK Column " + fkColumnName + " not found in columns: " + columns);
                           }

                           fkColumns.add(fkColumn);
                           refFkColumns.add(row.getString(3));
                        }
                        foreignKeys.add(new ForeignKey(fkName, fkColumns, fkTable, refFkColumns));
                        return foreignKeys;
                     });
         return Uni.combine().all().unis(pkUni, fksUni)
               // TODO: add unique constraints
               .combinedWith((pk, fks) -> new Table(table, columns, pk, fks, null))
               .onFailure().recoverWithNull();
      });
   }

   @Override
   String parameterName(String columnName, int pos) {
      return "$" + pos;
   }
}
