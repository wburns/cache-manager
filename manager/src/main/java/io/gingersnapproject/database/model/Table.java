package io.gingersnapproject.database.model;

import java.util.List;

public record Table(String name, List<Column> columns, PrimaryKey primaryKey, List<ForeignKey> foreignKeys, List<UniqueConstraint> uniqueConstraints) {
}
