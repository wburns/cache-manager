package io.gingersnapproject.database.vendor;

import io.gingersnapproject.database.model.Table;
import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Pool;

/**
 * @since 15.0
 **/
public interface Vendor {
   static Vendor fromDbKind(String dbKind) {
      return switch (dbKind.toLowerCase()) {
         case "postgresql":
         case "pgsql":
         case "pg":
            yield new PostgreSQLVendor();
         case "mssql":
            yield new MSSQLVendor();
         case "mysql":
         case "mariadb":
            yield new MySQLVendor();
         default:
            throw new UnsupportedOperationException();
      };
   }

   Uni<Table> describeTable(Pool pool, String table);
}
