package org.gbif.datarepo.resource;

import org.gbif.api.model.common.paging.PageableBase;

import javax.ws.rs.QueryParam;

import com.google.common.base.Strings;

/**
 * Wrapper class around Pageable to support paging parameters offset and limit.
 */
public class PagingParam extends PageableBase {

  /**
   * Sets the offset from a String value.
   * Empty or null values are ignored.
   */
  @QueryParam("offset")
  public void setOffset(String offset) {
    if (!Strings.isNullOrEmpty(offset)) {
      setOffset(Long.parseLong(offset));
    }
  }

  /**
   * Sets the limit from a String value.
   * Empty or null values are ignored.
   */
  @QueryParam("limit")
  public void setLimit(String limit) {
    if (!Strings.isNullOrEmpty(limit)) {
      setLimit(Integer.parseInt(limit));
    }
  }
}
