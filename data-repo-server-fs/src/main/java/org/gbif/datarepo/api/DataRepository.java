package org.gbif.datarepo.api;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.RepositoryStats;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Storage system for GBIF archives.
 */
public interface DataRepository {

  /**
   * Defines the mode of how an update operation has to be handled.
   */
  enum UpdateMode {

    /**
     * Append content to the data package.
     */
    APPEND,

    /**
     * Remove replace all the data package content.
     */
    OVERWRITE;
  }

  /**
   * Creates a DataPackage and store metadata and input content in the data repository.
   */
  DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files);

  /**
   * Updates/replaces all the contect of a DataPackage.
   */
  DataPackage update(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files, UpdateMode mode);


  /**
   * Deletes an archive associated to the input DOI.
   */
  void delete(DOI doi);

  /**
   * Hides a data package from search operations, effectively preventing its discovery during normal operations.
   */
  void archive(DOI doi);

  /**
   * Gets the path location from an archive associated to a DOI.
   */
  Optional<DataPackage> get(DOI doi);

  /**
   * Gets the path location from an archive associated to a DOI.
   */
  Optional<DataPackage> getByAlternativeIdentifier(String identifier);

  /**
   * List data packages optionally filtered by user and dates.
   */
  PagingResponse<DataPackage> list(@Nullable String user, @Nullable Pageable page, @Nullable Date fromDate,
                                   @Nullable Date toDate, Boolean deleted, @Nullable List<String> tags,
                                   @Nullable String q);

  /**
   * Gets a file contained in a data package referenced by a DOI.
   */
  Optional<DataPackageFile> getFile(DOI doi, String fileName);

  /**
   * Gets the file content by a DOI and fileName.
   */
  Optional<InputStream> getFileInputStream(DOI doi, String fileName);

  /**
   * Provides general statistics about the data repo usage.
   */
  RepositoryStats getStats();

}
