package io.gingersnapproject.database;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class GingersnapCommonProperties implements QuarkusTestResourceLifecycleManager {
   @Override
   public Map<String, String> start() {
      return Map.of(
            "gingersnap.rule.us-east.key-type", "PLAIN",
            "gingersnap.rule.us-east.plain-separator", ":",
            "gingersnap.rule.us-east.select-statement", "select fullname, email from customer where fullname = ?",
            "gingersnap.rule.us-east.connector.schema", "debezium",
            "gingersnap.rule.us-east.connector.table", "customer");
   }

   @Override
   public void stop() {

   }
}
