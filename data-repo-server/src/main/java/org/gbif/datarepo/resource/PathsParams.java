package org.gbif.datarepo.resource;

/**
 * Holds common paths and parameter names used across all services.
 */
public class PathsParams {

  /**
   * Private constructor.
   */
  private PathsParams() {
    //empty constructor
  }

  /**
   * Data packages resource parameter.
   */
  public static final String DATA_PACKAGES_PATH = "data_packages";

  /**
   * Metadata file parameter.
   */
  public static final String METADATA_PARAM = "metadata";

  /**
   * DataPackage content file parameter.
   */
  public static final String FILE_PARAM = "file";

}
