package io.gingersnapproject.database.model;

import java.util.List;

public record Table(String name, PrimaryKey primaryKey, List<ForeignKey> foreignKeys) {
}
