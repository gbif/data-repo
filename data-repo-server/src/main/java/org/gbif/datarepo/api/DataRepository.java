package org.gbif.datarepo.api;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Storage system for GBIF archives.
 */
public interface DataRepository {

  /**
   * Creates a DataPackage and store metadata and input content in the data repository.
   */
  DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files);

  /**
   * Stores the metadata file associated to a DOI.
   */
  void storeMetadata(DOI doi, InputStream file);

  /**
   * Deletes an archive associated to the input DOI.
   */
  void delete(DOI doi);

  /**
   * Gets the path location from an archive associated to a DOI.
   */
  Optional<DataPackage> get(DOI doi);

  /**
   * List data packages optionally filtered by user.
   */
  PagingResponse<DataPackage> list(@Nullable String user, @Nullable Pageable page);

  /**
   * Gets a file contained in a data package referenced by a DOI.
   */
  Optional<DataPackageFile> getFile(DOI doi, String fileName);

  /**
   * Gets the file content by a DOI and fileName.
   */
  Optional<InputStream> getFileInputStream(DOI doi, String fileName);

}
