package org.gbif.datarepo.persistence.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class TextArrayToSetTypeHandler extends BaseTypeHandler<Set<String>> {

  @Override
  public void setNonNullParameter(
    PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType
  ) throws SQLException {
    Array inArray = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, inArray);
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return asCollection(rs.getArray(columnName));
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return asCollection(rs.getArray(columnIndex));
  }

  @Override
  public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return asCollection(cs.getArray(columnIndex));
  }

  private static Set<String> asCollection(Array array) throws SQLException {
    if (array == null) {
      return null;
    }
    return new HashSet<>(Arrays.asList((String[])array.getArray()));
  }
}
