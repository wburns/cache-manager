package io.gingersnapproject.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MySQLResources implements QuarkusTestResourceLifecycleManager {
   private static final String IMAGE = "mysql:8.0.31";

   private static final String DATABASE = "gingersnap";

   private MySQLContainer<?> db;

   @Override
   public Map<String, String> start() {
      db = new MySQLContainer<>(IMAGE)
            .withUsername("gingersnap_user")
            .withPassword("password")
            .withDatabaseName(DATABASE)
            .withExposedPorts(MySQLContainer.MYSQL_PORT)
            .withCopyFileToContainer(MountableFile.forClasspathResource("mysql-setup.sql"), "/docker-entrypoint-initdb.d/mysql-setup.sql")
      ;
      db.start();

      Map<String, String> properties = new HashMap<>(Map.of(
            "quarkus.datasource.db-kind", "MYSQL",
            "quarkus.datasource.username", db.getUsername(),
            "quarkus.datasource.password", db.getPassword(),
            "quarkus.datasource.reactive.url", String.format("mysql://%s:%d/%s", db.getHost(), db.getMappedPort(MySQLContainer.MYSQL_PORT), db.getDatabaseName())));

      try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("mysql-test.properties")) {
         Properties p = new Properties();
         p.load(is);
         p.forEach((k, v) -> properties.put((String) k, (String) v));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      return properties;
   }

   @Override
   public void stop() {
      if (db != null)
         db.stop();
   }
}