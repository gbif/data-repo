package org.gbif.datarepo.inject;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.app.DataRepoConfigurationDW;
import org.gbif.datarepo.auth.basic.BasicAuthenticator;
import org.gbif.datarepo.auth.jwt.JwtAuthenticator;
import org.gbif.datarepo.persistence.DataPackageMyBatisModule;
import org.gbif.datarepo.persistence.mappers.IdentifierMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;
import org.gbif.datarepo.registry.DoiRegistrationWsClient;
import org.gbif.datarepo.store.fs.FileSystemRepository;
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

  private final DataRepoConfigurationDW configuration;
  private final Environment environment;

  private DoiRegistrationService doiRegistrationService;

  /**
   * Initializes mappers from the configuration settings and environment.
   */
  public DataRepoModule(DataRepoConfigurationDW configuration, Environment environment) {
    this.configuration = configuration;
    this.environment = environment;
    DataPackageMyBatisModule dataPackageMyBatisModule = new DataPackageMyBatisModule(configuration
                                                                                       .getDataRepoConfiguration()
                                                                                       .getDbConfig(),
                                                                                     environment.metrics(),
                                                                                     environment.healthChecks());
    injector = Guice.createInjector(new IdentityAccessModule(this.configuration.getDataRepoConfiguration().getUsersDb()),
                                    dataPackageMyBatisModule);
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
  public IdentifierMapper alternativeIdentifierMapper() {
    return injector.getInstance(IdentifierMapper.class);
  }

  /**
   * Gets RepositoryStatsMapper instance.
   */
  public RepositoryStatsMapper repositoryStatsMapper() {
    return injector.getInstance(RepositoryStatsMapper.class);
  }


  /**
   * Gets DataPackageDataPackageFileMapperMapper instance.
   */
  public DataPackageFileMapper dataPackageFileMapper() {
    return injector.getInstance(DataPackageFileMapper.class);
  }


  /**
   * Gets TagMapper instance.
   */
  public TagMapper tagMapper() {
    return injector.getInstance(TagMapper.class);
  }

  /**
   * Creates a new Authenticator instance using GBIF underlying services.
   */
  public Authenticator<BasicCredentials, GbifUserPrincipal> getBasicCredentialsAuthenticator() {
    return new BasicAuthenticator(injector.getInstance(IdentityAccessService.class));
  }

  /**
   * Creates a new Authenticator instance using GBIF underlying services.
   */
  public Authenticator<String, GbifUserPrincipal> getJWTAuthenticator() {
    return new JwtAuthenticator(configuration.getJwtAuthConfiguration(),
                                injector.getInstance(IdentityAccessService.class));
  }

  /**
   * Lazy creation of a DoiRegistrationService.
   * If the instance has ben created previously it is re-used.
   */
  public DoiRegistrationService doiRegistrationService() {
    if (doiRegistrationService == null) {
      Client client = DoiRegistrationWsClient.buildClient(configuration.getDataRepoConfiguration().getAppKey(),
                                                          environment.getObjectMapper());
      doiRegistrationService = new DoiRegistrationWsClient(buildWebTarget(client,
                                                                          configuration.getDataRepoConfiguration()
                                                                            .getGbifApiUrl()));
    }
    return doiRegistrationService;
  }

  /**
   * Creates an instance of DataRepository that is backed by a file system.
   */
  public DataRepository dataRepository() {
    return new FileSystemRepository(configuration.getDataRepoConfiguration().getDataRepoPath(),
                                    doiRegistrationService(),
                                    dataPackageMapper(),
                                    dataPackageFileMapper(), tagMapper(), repositoryStatsMapper(),
                                    alternativeIdentifierMapper(),
                                    configuration.getDataRepoConfiguration().getFileSystem());
  }

}
