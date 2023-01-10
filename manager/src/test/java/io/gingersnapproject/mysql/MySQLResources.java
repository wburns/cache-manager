package io.gingersnapproject.mysql;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.DATABASE;

public class MySQLResources implements DatabaseResourcesLifecyleManager.Database {

   private static final String IMAGE = "mysql:8.0.31";

   private MySQLContainer<?> db;

   @Override
   public void start() {
      db = new MySQLContainer<>(IMAGE)
              .withUsername("gingersnap_user")
              .withPassword("password")
              .withDatabaseName(DATABASE)
              .withExposedPorts(MySQLContainer.MYSQL_PORT)
              .withTmpFs(Map.of("/var/lib/mysql", "rw"))
              .withCopyFileToContainer(MountableFile.forClasspathResource("mysql/mysql-setup.sql"), "/docker-entrypoint-initdb.d/mysql-setup.sql")
              .withCopyFileToContainer(MountableFile.forClasspathResource("populate.sql"), "/docker-entrypoint-initdb.d/z_populate.sql");
      db.start();
   }

   @Override
   public void initProperties(Map<String, String> props) {
      props.put("quarkus.datasource.username", db.getUsername());
      props.put("quarkus.datasource.password", db.getPassword());
      props.put("quarkus.datasource.reactive.url", String.format("mysql://%s:%d/%s", db.getHost(), db.getMappedPort(MySQLContainer.MYSQL_PORT), db.getDatabaseName()));
      DatabaseResourcesLifecyleManager.loadProperties("mysql/mysql-test.properties", props);
   }


   @Override
   public void stop() {
      if (db != null)
         db.stop();
   }
}
