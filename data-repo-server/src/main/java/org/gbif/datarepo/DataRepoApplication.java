package org.gbif.datarepo;

import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.service.common.UserService;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.health.DataRepoHealthCheck;
import org.gbif.datarepo.health.AuthenticatorHealthCheck;
import org.gbif.datarepo.persistence.DataPackageMyBatisModule;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.registry.DoiRegistrationWsClient;
import org.gbif.datarepo.resource.DataPackageResource;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.discovery.lifecycle.DiscoveryLifeCycle;
import org.gbif.drupal.guice.DrupalMyBatisModule;

import javax.ws.rs.client.Client;

import static org.gbif.datarepo.conf.DataRepoConfiguration.USERS_DB_CONF_PREFIX;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * DropWizard application for the GBIF Data Repository.
 * This class initializes all the resources and services exposed in this application.
 */
public class DataRepoApplication extends Application<DataRepoConfiguration> {

  private static final String APPLICATION_NAME = "DataRepo";

  private Injector injector;

  public static void main(String[] args) throws Exception {
    new DataRepoApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  /**
   * Application entry point.
   */
  @Override
  public void run(DataRepoConfiguration configuration, Environment environment) throws Exception {
    injector  = injector(configuration, environment);


    // determines whether encountering of unknown properties (ones that do not map to a property, and there is no
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    environment.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    environment.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    Client client = DoiRegistrationWsClient.buildClient(configuration, environment.getObjectMapper());
    //Data repository creation
    DataRepository dataRepository = new FileSystemRepository(configuration,
                                                             new DoiRegistrationWsClient(DoiRegistrationWsClient.buildWebTarget(client, configuration.getGbifApiUrl())),
                                                             dataPackageMapper());
    //Security configuration
    Authenticator<BasicCredentials, UserPrincipal> authenticator = authenticator();
    BasicCredentialAuthFilter<UserPrincipal> userBasicCredentialAuthFilter =
      new BasicCredentialAuthFilter.Builder<UserPrincipal>().setAuthenticator(authenticator)
        .setRealm(GbifAuthenticator.GBIF_REALM).buildAuthFilter();
    environment.jersey().register(new AuthDynamicFeature(userBasicCredentialAuthFilter));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(UserPrincipal.class));

    //Resources and required features
    environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(new DataPackageResource(dataRepository, configuration));
    if (configuration.getService().isDiscoverable()) {
      environment.lifecycle().manage(new DiscoveryLifeCycle(configuration.getService()));
    }

    //Health checks
    environment.healthChecks().register("DataRepo", new DataRepoHealthCheck(configuration));
    environment.healthChecks().register("UserService", new AuthenticatorHealthCheck(authenticator));

  }

  @Override
  public void initialize(Bootstrap<DataRepoConfiguration> bootstrap) {
    // NOP
  }

  private Injector injector(DataRepoConfiguration configuration, Environment environment) {
    return Guice.createInjector(new DrupalMyBatisModule(configuration.getUsersDb()
                                                          .toProperties(USERS_DB_CONF_PREFIX)),
                                new DataPackageMyBatisModule(configuration.getDbConfig(),
                                                             environment.metrics(),
                                                             environment.healthChecks()));
  }

  /**
   * Creates a new Authenticator instance using GBIF underlying services.
   */
  private Authenticator<BasicCredentials, UserPrincipal> authenticator() {
    return new  GbifAuthenticator(injector.getInstance(UserService.class));
  }

  /**
   * Gets DataPackageMapper instance.
   */
  private DataPackageMapper dataPackageMapper() {
    return injector.getInstance(DataPackageMapper.class);
  }


}
