package org.gbif.datarepo.app;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.datarepo.auth.basic.BasicAuthenticator;
import org.gbif.datarepo.auth.jwt.JwtAuthConfiguration;
import org.gbif.datarepo.auth.jwt.JwtCredentialsFilter;
import org.gbif.datarepo.inject.DataRepoModule;
import org.gbif.datarepo.health.DataRepoHealthCheck;
import org.gbif.datarepo.health.AuthenticatorHealthCheck;
import org.gbif.datarepo.resource.DataPackageResource;
import org.gbif.datarepo.resource.RepositoryStatsResource;
import org.gbif.discovery.lifecycle.DiscoveryLifeCycle;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_METHODS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_HEADERS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOW_CREDENTIALS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_ORIGINS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.CHAIN_PREFLIGHT_PARAM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DropWizard application for the GBIF Data Repository.
 * This class initializes all the resources and services exposed in this application.
 */
public class DataRepoApplication extends Application<DataRepoConfigurationDW> {

  private static final Logger LOG = LoggerFactory.getLogger(DataRepoApplication.class);

  private static final String APPLICATION_NAME = "DataRepo";


  public static void main(String[] args) {
    try {
      new DataRepoApplication().run(args);
    } catch (Exception ex) {
      LOG.error("Error running application", ex);
      System.exit(1);
    }
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  /**
   * Registration of Authentication and authorization elements.
   */
  private static void registerSecurityComponents(DataRepoModule module, Environment environment,
                                                 JwtAuthConfiguration authJwtConfiguration) {
    environment.jersey().register(RolesAllowedDynamicFeature.class);
    //Security configuration
    Authenticator<BasicCredentials, GbifUserPrincipal> authenticator = module.getBasicCredentialsAuthenticator();
    BasicCredentialAuthFilter<GbifUserPrincipal> userBasicCredentialAuthFilter =
      new BasicCredentialAuthFilter.Builder<GbifUserPrincipal>()
        .setAuthenticator(module.getBasicCredentialsAuthenticator())
        .setRealm(BasicAuthenticator.GBIF_REALM).buildAuthFilter();
    JwtCredentialsFilter jwtCredentialsFilter = new JwtCredentialsFilter.Builder()
      .setConfiguration(authJwtConfiguration)
      .setAuthenticator(module.getJWTAuthenticator())
      .setRealm(BasicAuthenticator.GBIF_REALM).buildAuthFilter();
    environment.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(Lists.newArrayList(jwtCredentialsFilter,
                                                                                userBasicCredentialAuthFilter))));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(GbifUserPrincipal.class));

    //Health check
    environment.healthChecks().register("UserService", new AuthenticatorHealthCheck(authenticator));
  }

  /**
   * Application entry point.
   */
  @Override
  public void run(DataRepoConfigurationDW configuration, Environment environment) throws Exception {

    // Enforce use from ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    environment.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    environment.getObjectMapper().disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    environment.getObjectMapper().setDateFormat(new ISO8601DateFormat());

    DataRepoModule dataRepoModule = new DataRepoModule(configuration, environment);

    //CORS Filter
    FilterRegistration.Dynamic corsFilter = environment.servlets().addFilter("CORSFilter", CrossOriginFilter.class);
    corsFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    corsFilter.setInitParameter(ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,HEAD,OPTIONS");
    corsFilter.setInitParameter(ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
    corsFilter.setInitParameter(ALLOW_CREDENTIALS_PARAM, "true");
    corsFilter.setInitParameter(ALLOWED_ORIGINS_PARAM, "*");
    corsFilter.setInitParameter(CHAIN_PREFLIGHT_PARAM, Boolean.FALSE.toString());


    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    environment.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    //Register authentication and access control components
    registerSecurityComponents(dataRepoModule, environment, configuration.getJwtAuthConfiguration());

    //Resources and required features
    //environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(new DataPackageResource(dataRepoModule.dataRepository(), configuration,
                                                          environment.getValidator()));
    environment.jersey().register(new RepositoryStatsResource(dataRepoModule.dataRepository()));
    if (configuration.getService().isDiscoverable()) {
      environment.lifecycle().manage(new DiscoveryLifeCycle(configuration.getService()));
    }

    //Health checks
    environment.healthChecks().register("DataRepo", new DataRepoHealthCheck(configuration));

  }

  @Override
  public void initialize(Bootstrap<DataRepoConfigurationDW> bootstrap) {
    // NOP
    bootstrap.addBundle(new MultiPartBundle());
  }


}
