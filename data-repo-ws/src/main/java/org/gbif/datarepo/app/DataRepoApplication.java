package org.gbif.datarepo.app;

import org.gbif.api.model.common.UserPrincipal;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.inject.DataRepoModule;
import org.gbif.datarepo.health.DataRepoHealthCheck;
import org.gbif.datarepo.health.AuthenticatorHealthCheck;
import org.gbif.datarepo.resource.DataPackageResource;
import org.gbif.datarepo.resource.RepositoryStatsResource;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.discovery.lifecycle.DiscoveryLifeCycle;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_METHODS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOWED_HEADERS_PARAM;
import static org.eclipse.jetty.servlets.CrossOriginFilter.ALLOW_CREDENTIALS_PARAM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
   * Application entry point.
   */
  @Override
  public void run(DataRepoConfigurationDW configurationDW, Environment environment) throws Exception {
    DataRepoConfiguration configuration = configurationDW.getDataRepoConfiguration();
    DataRepoModule dataRepoModule = new DataRepoModule(configuration, environment);

    //CORS Filter
    FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORSFilter", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,
                                    environment.getApplicationContext().getContextPath() + "*");
    filter.setInitParameter(ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,HEAD,OPTIONS");
    filter.setInitParameter(ALLOWED_HEADERS_PARAM, "X-Requested-With, Origin, Content-Type, Accept");
    filter.setInitParameter(ALLOW_CREDENTIALS_PARAM, "true");


    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    environment.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use from ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    environment.getObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    //Security configuration
    Authenticator<BasicCredentials, UserPrincipal> authenticator = dataRepoModule.getAuthenticator();
    BasicCredentialAuthFilter<UserPrincipal> userBasicCredentialAuthFilter =
      new BasicCredentialAuthFilter.Builder<UserPrincipal>().setAuthenticator(dataRepoModule.getAuthenticator())
        .setRealm(GbifAuthenticator.GBIF_REALM).buildAuthFilter();
    environment.jersey().register(new AuthDynamicFeature(userBasicCredentialAuthFilter));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(UserPrincipal.class));

    //Resources and required features
    environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(new DataPackageResource(dataRepoModule.dataRepository(), configuration));
    environment.jersey().register(new RepositoryStatsResource(dataRepoModule.dataRepository()));
    if (configurationDW.getService().isDiscoverable()) {
      environment.lifecycle().manage(new DiscoveryLifeCycle(configurationDW.getService()));
    }

    //Health checks
    environment.healthChecks().register("DataRepo", new DataRepoHealthCheck(configuration));
    environment.healthChecks().register("UserService", new AuthenticatorHealthCheck(authenticator));

  }

  @Override
  public void initialize(Bootstrap<DataRepoConfigurationDW> bootstrap) {
    // NOP
  }


}
