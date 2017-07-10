package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage DataPackage instances.
 */
public interface DataPackageMapper {

  /**
   * Retrieves a DataPackage by its doi name, i.e.: prefix/suffix.
   */
  DataPackage get(@Param("doi") String doiName);

  /**
   * Page through DataPackages, optionally filtered by user and dates.
   */
  List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
                         @Nullable @Param("fromDate") Date fromDate, @Nullable @Param("toDate") Date toDate,
                         @Nullable @Param("deleted") Boolean deleted);

  /**
   * Count data packages, optionally filtered by user.
   */
  Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
             @Nullable @Param("fromDate") Date fromDate, @Nullable @Param("toDate") Date toDate,
             @Nullable @Param("deleted") Boolean deleted);

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
