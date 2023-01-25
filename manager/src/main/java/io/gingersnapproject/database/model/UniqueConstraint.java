package io.gingersnapproject.database.model;

import java.util.List;

public record UniqueConstraint(String name, List<Column> columns) {
}
