package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;

import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.UriBuilder;

/**
 * Utility class to generate URLs for data packages.
 */
public class DataPackageUriBuilder {

  private final UriBuilder uriBuilder;

  /**
   * DataPackages base API url.
   */
  public DataPackageUriBuilder(String baseUrl) {
    uriBuilder = UriBuilder.fromUri(baseUrl + "{suffix}/");
  }

  /**
   * Generates a DataPackage URL from the DOI.suffix.
   */
  public URI build(DOI doi) {
    return uriBuilder.build(doi.getDoiName());
  }

  /**
   * Generates a DataPackage URL from the DOI.suffix.
   */
  public URI build(UUID key) {
    return uriBuilder.build(key);
  }

  /**
   * Builds a URI using the supplied path.
   */
  public URI build(String path) {
    return uriBuilder.build(path);
  }
}
