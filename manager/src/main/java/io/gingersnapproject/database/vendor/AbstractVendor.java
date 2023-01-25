package io.gingersnapproject.database.vendor;

import java.util.List;

public abstract class AbstractVendor implements Vendor {
   @Override
   public String getSelectStatement(String tableName, List<String> keyColumns, List<String> valueColumns) {
      StringBuilder select = new StringBuilder("SELECT ");
      appendStrings(select, valueColumns, (key, pos) -> key, ", ");
      select.append(" FROM ").append(tableName);
      select.append(" WHERE ");
      appendStrings(select, keyColumns, (key, pos) -> key + " = " + parameterName(key, pos), " AND ");
      return select.toString();
   }

   String parameterName(String columnName, int pos) {
      return "?";
   }

   protected void appendStrings(StringBuilder sb, Iterable<String> strings, IntBiFunction<String, String> valueConversion,
         String separator) {
      boolean isFirst = true;
      int pos = 0;
      for (String columnName : strings) {
         if (!isFirst) {
            sb.append(separator);
         }
         sb.append(valueConversion.apply(columnName, pos++));
         isFirst = false;
      }
   }

   interface IntBiFunction<I, O> {
      O apply(I val, int intVal);
   }
}
