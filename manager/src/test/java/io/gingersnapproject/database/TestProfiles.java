package io.gingersnapproject.database;

import java.util.List;
import java.util.Set;

import io.gingersnapproject.database.mysql.MySqlTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;

public class TestProfiles {
   public static class MySqlTag implements QuarkusTestProfile {
      @Override
      public Set<String> tags() {
         return Set.of("mysql", "MYSQL");
      }

      @Override
      public List<TestResourceEntry> testResources() {
         return List.of(new TestResourceEntry(GingersnapCommonProperties.class),
               new TestResourceEntry(MySqlTestResourceLifecycleManager.class));
      }

      @Override
      public boolean disableGlobalTestResources() {
         return true;
      }
   }
}
