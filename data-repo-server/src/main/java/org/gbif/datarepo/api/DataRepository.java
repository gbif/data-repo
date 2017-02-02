package org.gbif.datarepo.api;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.model.DataPackage;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Storage system for GBIF archives.
 */
public interface DataRepository {

  /**
   * Creates a DataPackage and store metadata and input content in the data repository.
   */
  DataPackage create(String userName, InputStream metadata, List<FileInputContent> files);

  /**
   * Stores an archive file for the specified DOI.
   */
  void store(DOI doi, FileInputContent fileInputContent);

  /**
   * Stores the metadata file associated to a DOI.
   */
  void storeMetadata(DOI doi, InputStream file);

  /**
   * Deletes an archive associated to the input DOI.
   */
  void delete(DOI doi);

  /**
   * Gets the path location of an archive associated to a DOI.
   */
  Optional<DataPackage> get(DOI doi);

  /**
   * Gets a file contained in a data package referenced by a DOI.
   */
  Optional<File> getFile(DOI doi, String fileName);

  /**
   * Gets the file content by a DOI and fileName.
   */
  Optional<InputStream> getFileInputStream(DOI doi, String fileName);

}
