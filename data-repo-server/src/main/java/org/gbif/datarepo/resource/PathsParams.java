package org.gbif.datarepo.resource;

/**
 * Holds common paths and parameter names used across all services.
 */
public final class PathsParams {

  /**
   * Data packages resource path.
   */
  public static final String DATA_PACKAGES_PATH = "data_packages";

  /**
   * Data repository stats path.
   */
  public static final String REPO_STATS_PATH = DATA_PACKAGES_PATH + "/stats";

  /**
   * Metadata file parameter.
   */
  public static final String METADATA_PARAM = "metadata";

  /**
   * DataPackage content file parameter.
   */
  public static final String FILE_PARAM = "file";

  /**
   * DataPackage form parameters.
   */
  static final String DP_FORM_PARAM = "data_package";

  /**
   * Private constructor.
   */
  private PathsParams() {
    //empty constructor
  }


}
