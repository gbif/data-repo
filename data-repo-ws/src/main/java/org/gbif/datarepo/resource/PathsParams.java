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
   * Related identifiers resource path.
   */
  public static final String RELATED_IDENTIFIERS_PATH = "relatedIdentifiers";

  /**
   * Data repository stats path.
   */
  public static final String REPO_STATS_PATH = DATA_PACKAGES_PATH + "/stats";

  /**
   * DataPackage content file parameter.
   */
  public static final String FILE_PARAM = "file";

  /**
   * DataPackage content file URL parameter.
   */
  public static final String FILE_URL_PARAM = "fileUrl";


  /**
   * Identifiers file parameter.
   */
  public static final String IDENTIFIERS_FILE_PARAM = "identifiersFile";

  /**
   * Identifiers file URL parameter.
   */
  public static final String IDENTIFIERS_FILE_URL_PARAM = "identifiersFileUrl";

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
