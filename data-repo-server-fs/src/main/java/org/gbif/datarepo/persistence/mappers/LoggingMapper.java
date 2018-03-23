package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.persistence.model.DBLoggingEvent;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface LoggingMapper {

    List<DBLoggingEvent> list(@Nullable @Param("fromDate") Long fromDate,
                              @Nullable @Param("toDate") Long toDate,
                              @Nullable @Param("mdc") Map<String,String> mdc,
                              @Nullable @Param("page") Pageable page);

    Long count(@Nullable @Param("fromDate") Long fromDate,
               @Nullable @Param("toDate") Long toDate,
               @Nullable @Param("mdc") Map<String,String> mdc,
               @Nullable @Param("page") Pageable page);
}
