package io.gingersnapproject.database;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.gingersnapproject.Caches;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@TestProfile(TestProfiles.MySqlTag.class)
public class TestLazyLoading {
   @Inject
   Caches caches;

   @Test
   public void testEmptyRetrieval() {
      Uni<String> uni = caches.get("us-east", "bar");
      String value = uni.await().indefinitely();
      assertThat(value).isNotNull();
   }
}
