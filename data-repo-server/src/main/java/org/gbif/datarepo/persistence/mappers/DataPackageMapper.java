package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store DOIs and their status in the registry db.
 */
public interface DataPackageMapper {
  DataPackage get (@Param("doi") String doiName);
  List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page);
  Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page);
  void create (@Param("dataPackage") DataPackage dataPackage);
  void update (@Param("dataPackage") DataPackage dataPackage);
  void delete (@Param("doi") DOI doi);
}
