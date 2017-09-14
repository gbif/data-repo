package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.api.model.Tag;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface TagMapper {

  void create(Tag tag);

  void delete(@Param("tagKey") Integer tagKey);

  List<Tag> listByDoi(@Param("dataPackageDoi") DOI dataPackageDoi);
}
