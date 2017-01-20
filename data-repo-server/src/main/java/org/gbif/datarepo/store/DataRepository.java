package org.gbif.datarepo.store;

import org.gbif.api.model.common.DOI;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Storage system for GBIF archives.
 */
public interface DataRepository {

  /**
   * Stores an archive file for the specified DOI.
   */
  void store(DOI doi, InputStream file, String fileName);

  /**
   * Stores the metada file associated to a DOI.
   */
  void storeMetadata(DOI doi, InputStream file);

  /**
   * Deletes an archive associated to the input DOI.
   */
  void delete(DOI doi);

  /**
   * Gets the path location of an archive associated to a DOI.
   */
  Optional<Path> get(DOI doi);

  /**
   * Gets a file contained in a data package referenced by a DOI.
   */
  Optional<InputStream> getFile(DOI doi, String fileName);

}
