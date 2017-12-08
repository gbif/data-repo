package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage DataPackage instances.
 */
public interface DataPackageMapper {

  /**
   * Retrieves a DataPackage by its doi name, i.e.: prefix/suffix.
   */
  DataPackage getByDOI(@Param("doi") String doiName);

  /**
   * Retrieves a DataPackage by its key.
   */
  DataPackage getByKey(@Param("dataPackageKey") UUID dataPackageKey);

  DataPackage getByAlternativeIdentifier(@Param("identifier") String identifier);

  /**
   * Page through DataPackages, optionally filtered by user and dates.
   */
  List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
                         @Nullable @Param("fromDate") Date fromDate, @Nullable @Param("toDate") Date toDate,
                         @Nullable @Param("deleted") Boolean deleted, @Nullable @Param("tags") List<String> tags,
                         @Nullable @Param("publishedIn") String publishedIn, @Nullable @Param("shareIn") String shareIn,
                         @Nullable @Param("query") String q);

  /**
   * Count data packages, optionally filtered by user.
   */
  Long count(@Nullable @Param("user") String user, @Nullable @Param("fromDate") Date fromDate,
             @Nullable @Param("toDate") Date toDate, @Nullable @Param("deleted") Boolean deleted,
             @Nullable @Param("tags") List<String> tags, @Nullable @Param("publishedIn") String publishedIn,
             @Nullable @Param("shareIn") String shareIn, @Nullable @Param("query") String q);

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
  void delete(@Param("dataPackageKey") UUID dataPackageKey);

  /**
   * Deletes a data package by its doi value.
   */
  void archive(@Param("dataPackageKey") UUID dataPackageKey);
}
