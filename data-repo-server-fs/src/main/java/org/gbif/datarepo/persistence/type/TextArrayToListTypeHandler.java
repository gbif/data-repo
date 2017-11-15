package org.gbif.datarepo.persistence.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class TextArrayToListTypeHandler extends BaseTypeHandler<List<String>> {

  @Override
  public void setNonNullParameter(
    PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType
  ) throws SQLException {
    Array inArray = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, inArray);
  }

  @Override
  public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return asCollection(rs.getArray(columnName));
  }

  @Override
  public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return asCollection(rs.getArray(columnIndex));
  }

  @Override
  public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return asCollection(cs.getArray(columnIndex));
  }

  private static List<String> asCollection(Array array) throws SQLException {
    if (array == null) {
      return null;
    }
    return Arrays.asList((String[])array.getArray());
  }
}
