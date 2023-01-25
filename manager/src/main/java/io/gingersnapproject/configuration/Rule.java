package io.gingersnapproject.configuration;

import java.util.List;
import java.util.Optional;

import org.infinispan.commons.dataconversion.internal.Json;

import io.smallrye.config.WithDefault;

public interface Rule {

      Connector connector();

      @WithDefault("PLAIN")
      KeyType keyType();

      @WithDefault("|")
      String plainSeparator();

      @WithDefault("false")
      boolean queryEnabled();


      @WithDefault("true")
      boolean expandEntity();

      enum KeyType {
            PLAIN {
                  @Override
                  public String[] toArguments(String strValue, String plainSeparator) {
                        return strValue.split(plainSeparator);
                  }
            },
            JSON {
                  @Override
                  public String[] toArguments(String strValue, String plainSeparator) {
                        return Json.read(strValue)
                              .asJsonMap()
                              .values()
                              .stream()
                              .map(Json::toString)
                              .toArray(String[]::new);
                  }
            };
            public abstract String[] toArguments(String strValue, String plainSeparator);
      }
}
