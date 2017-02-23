package org.gbif.datarepo.registry;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.datarepo.auth.GbifAuthService;
import org.gbif.datarepo.auth.HttpGbifAuthFilter;
import org.gbif.datarepo.conf.DataRepoConfiguration;
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

public class DoiRegistrationWsClient implements DoiRegistrationService {

  private final WebTarget webTarget;

  public DoiRegistrationWsClient(WebTarget webTarget) {
    this.webTarget = webTarget;
  }

  protected Invocation.Builder request(WebTarget decoratedWebTarget) {
    return decoratedWebTarget.request(MediaType.APPLICATION_JSON_TYPE);
  }

  @Override
  public DOI generate(DoiType doiType) {
    return request(webTarget.path("gen").path(doiType.name())).post(Entity.json(null), DOI.class);
  }

  @Override
  public DoiData get(String prefix, String suffix) {
    return request(webTarget.path(prefix).path(suffix)).get(DoiData.class);
  }

  @Override
  public void delete(String prefix, String suffix) {
    request(webTarget.path(prefix).path(suffix)).delete();
  }

  @Override
  public DOI register(DoiRegistration doiRegistration) {
    return request(webTarget).header(GbifAuthService.HEADER_GBIF_USER, doiRegistration.getUser())
      .post(Entity.json(doiRegistration), DOI.class);
  }

  public static Client buildClient(DataRepoConfiguration configuration, ObjectMapper mapper) {
    ClientConfig clientConfig = new ClientConfig().register(JacksonObjectMapperProvider.class)
                                  .register(JacksonFeature.class)
                                  .register(new HttpGbifAuthFilter(configuration.getAppKey(), mapper));
    return ClientBuilder.newClient(clientConfig);
  }

  public static WebTarget buildWebTarget(Client client, String apiUrl) {
    return client.target(apiUrl + "doi");
  }

}
