package io.gingersnapproject.configuration;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface Connector {

   String schema();

   String table();

   Optional<String> selectStatement();

   Optional<List<String>> keyColumns();

   Optional<List<String>> valueColumns();
}
