package io.gingersnapproject.database.model;

import java.util.List;

public record PrimaryKey(String name, List<String> columns) {
}
