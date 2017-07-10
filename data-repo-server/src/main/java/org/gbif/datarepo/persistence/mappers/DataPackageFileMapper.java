package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage DataPackageFile instances.
 */
public interface DataPackageFileMapper {

  /**
   * Retrieves a DataPackageFile by its doi name and fileName, i.e.: prefix/suffix.
   */
  DataPackageFile get(@Param("doi") DOI doi, @Param("fileName") String fileName);

  /**
   * List DataPackageFiles, optionally filtered by doi.
   */
  List<DataPackageFile> list(@Nullable @Param("doi") DOI doi);


  /**
   * Persists a new data package file.
   */
  void create(@Param("doi") DOI doi, @Param("dpf") DataPackageFile dataPackageFile);

  /**
   * Archive an existing package file.
   */
  void archive(@Param("doi") DOI doi);

  /**
   * Updates a data package file.
   */
  void update(@Param("doi") String doiName, @Param("dpf") DataPackageFile dataPackageFile);

  /**
   * Deletes a data package file by its doi and name.
   */
  void delete(@Param("doi") DOI doi, @Param("fileName") String fileName);
}
