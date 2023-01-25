package io.gingersnapproject.database.model;

import java.util.List;

public record ForeignKey(String name, List<Column> columns, String refTable, List<String> refColumns) {
}
