package org.gbif.datarepo.persistence.mappers;

import org.gbif.datarepo.api.model.DataPackageFile;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage DataPackageFile instances.
 */
public interface DataPackageFileMapper {

  /**
   * Retrieves a DataPackageFile by its doi name and fileName, i.e.: prefix/suffix.
   */
  DataPackageFile get(@Param("dataPackageKey") UUID dataPackageKey, @Param("fileName") String fileName);

  /**
   * List DataPackageFiles, optionally filtered by doi.
   */
  List<DataPackageFile> list(@Nullable @Param("dataPackageKey") UUID dataPackageKey);


  /**
   * Persists a new data package file.
   */
  void create(@Param("dataPackageKey") UUID dataPackageKey, @Param("dpf") DataPackageFile dataPackageFile);

  /**
   * Archive an existing package file.
   */
  void archive(@Param("dataPackageKey") UUID dataPackageKey);

  /**
   * Updates a data package file.
   */
  void update(@Param("dataPackageKey") UUID dataPackageKey, @Param("dpf") DataPackageFile dataPackageFile);

  /**
   * Deletes a data package file by its doi and name.
   */
  void delete(@Param("dataPackageKey") UUID dataPackageKey, @Param("fileName") String fileName);
}
