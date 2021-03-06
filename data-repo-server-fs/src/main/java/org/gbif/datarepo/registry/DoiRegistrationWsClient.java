package org.gbif.datarepo.registry;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.datarepo.auth.HttpGbifClientAuthFilter;
import org.gbif.datarepo.impl.conf.ApplicationKeyConfiguration;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Jersey 2 client implementation for the DoiRegistrationService.
 * This class is implemented here since the available client is implemented for Jersey 1 and bring it to this project
 * created classpath dependencies issues.
 */
public class DoiRegistrationWsClient implements DoiRegistrationService {

  //Path to the doi generation service
  private static final String GEN_PATH = "gen";

  //Base path to the doi registration service
  private static final String DOI_PATH = "doi";

  //Base web target that point to the doi registration url
  private final WebTarget webTarget;

  public DoiRegistrationWsClient(WebTarget webTarget) {
    this.webTarget = webTarget;
  }

  /**
   * Creates a Invocation.Builder from a web target.
   */
  protected static Invocation.Builder request(WebTarget decoratedWebTarget) {
    return decoratedWebTarget.request(MediaType.APPLICATION_JSON_TYPE);
  }

  /**
   * Generates a new DoiType.
   */
  @Override
  public DOI generate(DoiType doiType) {
    return request(webTarget.path(GEN_PATH).path(doiType.name())).post(Entity.json(null), DOI.class);
  }

  /**
   * Retrieves the DOI data.
   */
  @Override
  public DoiData get(String prefix, String suffix) {
    return request(webTarget.path(prefix).path(suffix)).get(DoiData.class);
  }

  /**
   * Deletes an existing DOI.
   */
  @Override
  public void delete(String prefix, String suffix) {
    request(webTarget.path(prefix).path(suffix)).delete();
  }

  /**
   * Requests the registration from a new DOI.
   */
  @Override
  public DOI register(DoiRegistration doiRegistration) {
    return request(webTarget).post(Entity.json(doiRegistration), DOI.class);
  }

  /**
   * Requests the registration from a new DOI.
   */
  @Override
  public DOI update(DoiRegistration doiRegistration) {
    return request(webTarget).put(Entity.json(doiRegistration), DOI.class);
  }

  /**
   * Builds a Jersey Client that uses a custom JacksonObjectMapperProvider and the HttpGbifAuthFilter.
   */
  public static Client buildClient(ApplicationKeyConfiguration applicationKeyConfiguration, ObjectMapper mapper) {
    ClientConfig clientConfig = new ClientConfig().register(JacksonObjectMapperProvider.class)
                                  .register(JacksonFeature.class)
                                  .register(new HttpGbifClientAuthFilter(applicationKeyConfiguration, mapper));
    return ClientBuilder.newClient(clientConfig);
  }

  /**
   * Build a new web target that points to the doi registration service.
   */
  public static WebTarget buildWebTarget(Client client, String apiUrl) {
    return client.target(apiUrl + DOI_PATH);
  }

}
