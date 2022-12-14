package io.gingersnapproject.database.mysql;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MySqlTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

   public static final String PRIVILEGED_USER = "gingersnap_user";
   public static final String PRIVILEGED_PASSWORD = "mysqlpassword";
   public static final String ROOT_PASSWORD = "debezium";
   public static final String DBNAME = "debezium";
   public static final String IMAGE = "mysql:8.0.31";
   public static final Integer PORT = 3306;

   private static final GenericContainer<?> container = new GenericContainer<>(IMAGE)
         .waitingFor(Wait.forLogMessage(".*mysqld: ready for connections.*", 2))
         .withEnv("MYSQL_ROOT_PASSWORD", ROOT_PASSWORD)
         .withEnv("MYSQL_USER", PRIVILEGED_USER)
         .withEnv("MYSQL_PASSWORD", PRIVILEGED_PASSWORD)
         .withCopyFileToContainer(MountableFile.forClasspathResource("mysql/setup.sql"), "/docker-entrypoint-initdb.d/setup.sql")
         .withExposedPorts(PORT)
         .withStartupTimeout(Duration.ofSeconds(180));

   public static GenericContainer<?> getContainer() {
      return container;
   }

   @Override
   public Map<String, String> start() {
      container.start();

      Map<String, String> params = new HashMap<>();

      params.put("quarkus.datasource.db-kind", "MYSQL");
      params.put("quarkus.datasource.username", PRIVILEGED_USER);
      params.put("quarkus.datasource.password", PRIVILEGED_PASSWORD);
      params.put("quarkus.datasource.reactive.url", "mysql://" + container.getHost() + ":" + container.getMappedPort(3306) + "/" + DBNAME);

      return params;
   }

   @Override
   public void stop() {
      try {
         if (container != null) {
            container.stop();
         }
      }
      catch (Exception e) {
         // ignored
      }
   }
}
