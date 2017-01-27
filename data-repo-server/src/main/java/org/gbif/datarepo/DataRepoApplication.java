package org.gbif.datarepo;

import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.service.common.UserService;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.conf.DataCiteConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.health.DataCiteHealthCheck;
import org.gbif.datarepo.health.DataRepoHealthCheck;
import org.gbif.datarepo.health.AuthenticatorHealthCheck;
import org.gbif.datarepo.resource.DataPackageResource;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.drupal.guice.DrupalMyBatisModule;
import org.gbif.utils.HttpUtil;

import static org.gbif.datarepo.conf.DataRepoConfiguration.USERS_DB_CONF_PREFIX;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Environment;
import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * DropWizard application for the GBIF Data Repository.
 * This class initializes all the resources and services exposed in this application.
 */
public class DataRepoApplication extends Application<DataRepoConfiguration> {

  private static final String APPLICATION_NAME = "DataRepo";

  private static final String GBIF_REALM = "GBIF";

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
    //Data repository creation
    DataRepository dataRepository = new FileSystemRepository(configuration.getDataRepoPath());
    //DataCite and DOI services
    DataCiteService dataCiteService = dataCiteService(configuration.getDataCiteConfiguration());
    DataPackagesDoiGenerator doiGenerator = new DataPackagesDoiGenerator(configuration.getDoiCommonPrefix(),
                                                                         configuration.getDoiShoulder(),
                                                                         dataCiteService);

    //Security configuration
    Authenticator<BasicCredentials, UserPrincipal> authenticator = authenticator(configuration);
    BasicCredentialAuthFilter<UserPrincipal> userBasicCredentialAuthFilter =
      new BasicCredentialAuthFilter.Builder<UserPrincipal>().setAuthenticator(authenticator)
        .setRealm(GBIF_REALM).buildAuthFilter();
    environment.jersey().register(new AuthDynamicFeature(userBasicCredentialAuthFilter));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(UserPrincipal.class));

    //Resources and required features
    environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(new DataPackageResource(dataRepository, dataCiteService, doiGenerator,
                                                          configuration));

    //Health checks
    environment.healthChecks().register("DataCite", new DataCiteHealthCheck(dataCiteService, doiGenerator));
    environment.healthChecks().register("DataRepo", new DataRepoHealthCheck(configuration));
    environment.healthChecks().register("UserService", new AuthenticatorHealthCheck(authenticator));
  }

  /**
   * Creates a new Authenticator instance using GBIF underlying services.
   */
  private static Authenticator<BasicCredentials, UserPrincipal> authenticator(DataRepoConfiguration configuration) {
    Injector injector = Guice.createInjector(new DrupalMyBatisModule(configuration.getUsersDb()
                                                                       .toProperties(USERS_DB_CONF_PREFIX)));
    return new  GbifAuthenticator(injector.getInstance(UserService.class));
  }

  /**
   * Creates a new DataCiteService instance using the provided configuration.
   */
  private static DataCiteService dataCiteService(DataCiteConfiguration dataCiteConfiguration) {
    return new DataCiteService(httpClient(dataCiteConfiguration), dataCiteConfiguration.asServiceConfiguration());
  }

  /**
   * Creates a new HttpClient, it's required by the DataCiteService.
   */
  private static CloseableHttpClient httpClient(DataCiteConfiguration configuration) {
    return HttpUtil.newMultithreadedClient(configuration.getTimeout(), configuration.getThreads(), configuration.getThreads());
  }

}
