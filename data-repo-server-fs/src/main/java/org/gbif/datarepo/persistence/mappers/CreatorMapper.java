package org.gbif.datarepo.persistence.mappers;

import org.gbif.datarepo.api.model.Creator;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface CreatorMapper {

  void create(Creator creator);

  void delete(@Param("creatorKey") Integer creatorKey);

  List<Creator> listByDataPackageKey(@Param("dataPackageKey") UUID dataPackageKey);
}
