package org.gbif.datarepo.auth;

import org.gbif.datarepo.conf.ApplicationKeyConfiguration;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GBIF Jersey 2 client authentication filter.
 */
public class HttpGbifAuthFilter implements ClientRequestFilter {

  private final GbifAuthService authService;

  /**
   * By default uses an application key and secret to sing requests.
   */
  public HttpGbifAuthFilter(ApplicationKeyConfiguration appKeyConfiguration, ObjectMapper mapper) {
    authService = GbifAuthService.singleKeyAuthService(appKeyConfiguration.getAppKey(),
                                                       appKeyConfiguration.getAppSecretKey(), mapper);

  }

  /**
   * Sing the requestContext.
   */
  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    authService.signRequest(requestContext);
  }
}
