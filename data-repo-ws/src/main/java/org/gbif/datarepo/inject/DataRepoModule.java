package org.gbif.datarepo.inject;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.persistence.DataPackageMyBatisModule;
import org.gbif.datarepo.persistence.mappers.AlternativeIdentifierMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.registry.DoiRegistrationWsClient;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.identity.inject.IdentityAccessModule;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import javax.ws.rs.client.Client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Environment;

import static org.gbif.datarepo.registry.DoiRegistrationWsClient.buildWebTarget;

/**
 * Factory of MyBatis mappers and services default services.
 */
public class DataRepoModule {

  //Guice injector
  private final Injector injector;

  private final DataRepoConfiguration configuration;
  private final Environment environment;

  private DoiRegistrationService doiRegistrationService;

  /**
   * Initializes mappers from the configuration settings and environment.
   */
  public DataRepoModule(DataRepoConfiguration configuration, Environment environment) {
    this.configuration = configuration;
    this.environment = environment;
    DataPackageMyBatisModule dataPackageMyBatisModule = new DataPackageMyBatisModule(configuration.getDbConfig(),
                                                                                     environment.metrics(),
                                                                                     environment.healthChecks());
    injector = Guice.createInjector(new IdentityAccessModule(configuration.getUsersDb()), dataPackageMyBatisModule);
  }

  /**
   * Gets DataPackageMapper instance.
   */
  public DataPackageMapper dataPackageMapper() {
    return injector.getInstance(DataPackageMapper.class);
  }

  /**
   * Gets AlternativeIdentifierMapper instance.
   */
  public AlternativeIdentifierMapper alternativeIdentifierMapper() {
    return injector.getInstance(AlternativeIdentifierMapper.class);
  }

  /**
   * Gets RepositoryStatsMapper instance.
   */
  public RepositoryStatsMapper repositoryStatsMapper() {
    return injector.getInstance(RepositoryStatsMapper.class);
  }


  /**
   * Gets DataPackageMapper instance.
   */
  public DataPackageFileMapper dataPackageFileMapper() {
    return injector.getInstance(DataPackageFileMapper.class);
  }
  /**
   * Creates a new Authenticator instance using GBIF underlying services.
   */
  public Authenticator<BasicCredentials, GbifUserPrincipal> getAuthenticator() {
    return new GbifAuthenticator(injector.getInstance(IdentityAccessService.class));
  }

  /**
   * Lazy creation of a DoiRegistrationService.
   * If the instance has ben created previously it is re-used.
   */
  public DoiRegistrationService doiRegistrationService() {
    if (doiRegistrationService == null) {
      Client client = DoiRegistrationWsClient.buildClient(configuration, environment.getObjectMapper());
      doiRegistrationService = new DoiRegistrationWsClient(buildWebTarget(client, configuration.getGbifApiUrl()));
    }
    return doiRegistrationService;
  }

  /**
   * Creates an instance of DataRepository that is backed by a file system.
   */
  public DataRepository dataRepository() {
    return new FileSystemRepository(configuration, doiRegistrationService(), dataPackageMapper(),
                                    dataPackageFileMapper(), repositoryStatsMapper(), alternativeIdentifierMapper());
  }

}
