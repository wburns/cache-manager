package io.gingersnapproject.rules;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.dataconversion.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.gingersnapproject.mysql.MySQLResources;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = DatabaseResourcesLifecyleManager.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JoinTest {
   private static final String RULE_NAME = "flight";
   private static final String GET_PATH = "/rules/{rule}/{key}";
   private RemoteCacheManager cm;
   private RemoteCache<String, String> cache;

   @BeforeAll
   public void beforeAll() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer()
            .host("127.0.0.1")
            .port(11222);

      cm = new RemoteCacheManager(builder.build());
      cache = cm.getCache(RULE_NAME)
            .withDataFormat(DataFormat.builder().keyType(MediaType.TEXT_PLAIN).valueType(MediaType.TEXT_PLAIN).build());
   }

   @AfterEach
   public void afterAll() {
      if (cm != null) {
         cm.stop();
      }

      cm = null;
      cache = null;
   }


   @Test
   public void testJoin() {
      cache.put("3", "{\"name\":\"BA0666\", \"scheduled_time\":\"12:00:00\", \"airline_id\": \"1\", \"gate_id\":\"3\"}");
      given().when().get(GET_PATH, "flight", "3").then().body(containsString("British Airways")).body(containsString("B1"));
   }
}
