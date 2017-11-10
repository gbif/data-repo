package org.gbif.datarepo.persistence.mappers;

import org.gbif.datarepo.api.model.Tag;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface TagMapper {

  void create(Tag tag);

  void delete(@Param("tagKey") Integer tagKey);

  List<Tag> listByDataPackageKey(@Param("dataPackageKey") UUID dataPackageKey);
}
