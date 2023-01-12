package io.gingersnapproject.database.model;

import java.util.List;

public record ForeignKey(String name, List<String> columns, String refTable, List<String> refColumns) {
}
