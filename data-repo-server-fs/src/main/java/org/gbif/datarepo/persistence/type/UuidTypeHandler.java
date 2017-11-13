package org.gbif.datarepo.persistence.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UuidTypeHandler extends BaseTypeHandler<UUID> {
  private static final Logger LOG = LoggerFactory.getLogger(UuidTypeHandler.class);

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
    ps.setObject(i, parameter.toString(), Types.OTHER);
  }

  @Override
  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toUUID(rs.getString(columnName));
  }

  @Override
  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toUUID(rs.getNString(columnIndex));
  }

  @Override
  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toUUID(cs.getString(columnIndex));
  }

  private static UUID toUUID(String val) {
    if (Strings.isNullOrEmpty(val)) {
      return null;
    }
    try {
      return UUID.fromString(val);
    } catch (IllegalArgumentException e) {
      LOG.warn("Bad UUID found: {}", val);
    }
    return null;
  }

}
