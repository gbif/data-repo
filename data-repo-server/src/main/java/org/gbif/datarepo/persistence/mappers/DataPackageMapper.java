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

  /**
   * Retrieves a DataPackage by its doi name, i.e.: prefix/suffix.
   */
  DataPackage get(@Param("doi") String doiName);

  /**
   * Page through DataPackages, optionally filtered by user.
   */
  List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page);

  /**
   * Count data packages, optionally filtered by user.
   */
  Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page);

  /**
   * Persists a new data package.
   */
  void create(DataPackage dataPackage);

  /**
   * Updates a data package.
   */
  void update(DataPackage dataPackage);

  /**
   * Deletes a data package by its doi value.
   */
  void delete(@Param("doi") DOI doi);
}
