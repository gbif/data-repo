package org.gbif.datarepo.auth;

import org.gbif.datarepo.impl.conf.ApplicationKeyConfiguration;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GBIF Jersey 2 client authentication filter.
 */
public class HttpGbifClientAuthFilter implements ClientRequestFilter {

  private final GbifAuthService authService;

  /**
   * By default uses an application key and secret to sing requests.
   */
  public HttpGbifClientAuthFilter(ApplicationKeyConfiguration appKeyConfiguration, ObjectMapper mapper) {
    authService = GbifAuthService.singleKeyAuthService(appKeyConfiguration.getAppKey(),
                                                       appKeyConfiguration.getAppSecretKey(),
                                                       mapper,
                                                       appKeyConfiguration.isSelfAuthenticated());

  }

  /**
   * Sing the requestContext.
   */
  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    authService.signRequest(requestContext);
  }
}
