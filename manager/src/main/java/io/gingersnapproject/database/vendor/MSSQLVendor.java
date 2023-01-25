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

public class MSSQLVendor extends AbstractVendor {

    private static final Logger log = LoggerFactory.getLogger(MSSQLVendor.class);

    @Override
    public Uni<Table> describeTable(Pool pool, String table) {
       Uni<List<Column>> uni = prepareQuery(pool, "EXEC sp_columns @P1;")
             .execute(Tuple.of(table))
             .onFailure().invoke(t -> log.error("Error retrieving table description for " + table, t))
             .map(rs -> {
                List<Column> columns = new ArrayList<>();
                // Column 1: Table Owner
                // Column 2: Table Name
                // Column 3: Column Name
                // Column 4: Data Type
                // Column 5: Type Name
                // Column 6: Precision
                // Column 7: Length
                for (Row row : rs) {
                   String columnName = row.getString(3);
                   String columnType = row.getString(5);
                   columns.add(new Column(columnName, JavaType.fromString(columnType)));
                }
                return columns;
             });

       return uni.onItem().transformToUni(columns -> {
          Map<String, Column> columnMap = columns.stream().collect(Collectors.toMap(c -> c.name().toUpperCase(), Function.identity()));
          String pkSQL =
                """
                      SELECT
                           KU.table_name as TABLENAME
                          ,column_name as PRIMARYKEYCOLUMN
                      FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS AS TC

                      INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KU
                          ON TC.CONSTRAINT_TYPE = 'PRIMARY KEY'
                          AND TC.CONSTRAINT_NAME = KU.CONSTRAINT_NAME
                          AND KU.table_name = @P1

                      ORDER BY
                           KU.TABLE_NAME
                          ,KU.ORDINAL_POSITION
                      ;""";
          Uni<PrimaryKey> pkUni =
                prepareQuery(pool, pkSQL)
                      .execute(Tuple.of(table))
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

          String fkSQL =
                """
                      SELECT  obj.name AS FK_NAME,
                          sch.name AS [schema_name],
                          tab1.name AS [table],
                          col1.name AS [column],
                          tab2.name AS [referenced_table],
                          col2.name AS [referenced_column]
                      FROM sys.foreign_key_columns fkc
                      INNER JOIN sys.objects obj
                          ON obj.object_id = fkc.constraint_object_id
                      INNER JOIN sys.tables tab1
                          ON tab1.object_id = fkc.parent_object_id
                      INNER JOIN sys.schemas sch
                          ON tab1.schema_id = sch.schema_id
                      INNER JOIN sys.columns col1
                          ON col1.column_id = parent_column_id AND col1.object_id = tab1.object_id
                      INNER JOIN sys.tables tab2
                          ON tab2.object_id = fkc.referenced_object_id
                      INNER JOIN sys.columns col2
                          ON col2.column_id = referenced_column_id AND col2.object_id = tab2.object_id
                      WHERE tab1.name = @P1""";

          Uni<List<ForeignKey>> fksUni =
                prepareQuery(pool, fkSQL)
                      .execute(Tuple.of(table))
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
                            fkTable = row.getString(4);
                            String fkColumnName = row.getString(3).toUpperCase();
                            Column pkColumn = columnMap.get(fkColumnName);
                            if (pkColumn == null) {
                               throw new IllegalStateException("FK Column " + fkColumnName + " not found in columns: " + columns);
                            }

                            fkColumns.add(pkColumn);
                            refFkColumns.add(row.getString(5));
                         }
                         foreignKeys.add(new ForeignKey(fkName, fkColumns, fkTable, refFkColumns));
                         return foreignKeys;
                      });

          return Uni.combine().all().unis(pkUni, fksUni)
                .combinedWith((pk, fks) ->  new Table(table, columns, pk, fks, null)).onFailure().recoverWithNull();
       });
    }

   @Override
   String parameterName(String columnName, int pos) {
      return "@P" + pos;
   }
}
