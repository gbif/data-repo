package org.gbif.datarepo.persistence.type;

import org.gbif.api.model.common.DOI;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Transforms a DOI into a String representation using the method doi.getDoiName.
 */
public class DoiTypeHandler extends BaseTypeHandler<DOI> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, DOI parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, parameter.getDoiName());
  }

  @Override
  public DOI getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toDoi(rs.getString(columnName));
  }

  @Override
  public DOI getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toDoi(rs.getString(columnIndex));
  }

  @Override
  public DOI getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toDoi(cs.getString(columnIndex));
  }

  /**
   * Transforms a String in the form "10.5072/dp.sbrfjh" in to instance DOI(10.5072,dp.sbrfjh).
   */
  private static DOI toDoi(String value) {
    String[] doi = value.split("\\/");
    return new DOI(doi[0], doi[1]);
  }
}
