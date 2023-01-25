package io.gingersnapproject.database.model;

import java.sql.Types;

public enum JavaType {
   STRING {
      @Override
      Object fromStringValue(String value) {
         return value;
      }
   },
   INT {
      @Override
      Object fromStringValue(String value) {
         return Integer.valueOf(value);
      }
   },
   LONG {
      @Override
      Object fromStringValue(String value) {
         return Long.valueOf(value);
      }
   },
   DOUBLE {
      @Override
      Object fromStringValue(String value) {
         return Double.valueOf(value);
      }
   },
   BOOLEAN {
      @Override
      Object fromStringValue(String value) {
         return Boolean.getBoolean(value);
      }
   },
   DATE,
   BYTEARRAY;

   Object fromStringValue(String value) {
      throw new UnsupportedOperationException("Not supported yet!");
   }

   public static JavaType fromInt(int type) {
      return switch (type) {
         case Types.CHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.VARCHAR, Types.LONGVARCHAR -> STRING;
         case Types.INTEGER, Types.SMALLINT -> INT;
         case Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> DOUBLE;
         case Types.BOOLEAN, Types.BIT -> BOOLEAN;
         case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> BYTEARRAY;
         case Types.DATE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> DATE;
         default -> throw new UnsupportedOperationException("ODBC SQL Type " + type + " not supported");

      };
   }

   public static JavaType fromString(String type) {
      type = type.toUpperCase();
      if (type.contains("BINARY") || type.contains("BLOB")) {
         return BYTEARRAY;
      }
      if (type.contains("BOOL")) {
         return BOOLEAN;
      }
      if (type.contains("INT")) {
         return INT;
      }
      if (type.contains("LONG")) {
         return LONG;
      }

      return STRING;
   }
}
