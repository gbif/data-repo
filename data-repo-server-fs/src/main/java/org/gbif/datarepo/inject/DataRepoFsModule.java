package org.gbif.datarepo.inject;

import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.persistence.DataPackageMyBatisModule;
import org.gbif.datarepo.persistence.mappers.AlternativeIdentifierMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.registry.DoiRegistrationWsClient;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import javax.ws.rs.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.gbif.datarepo.registry.DoiRegistrationWsClient.buildWebTarget;

/**
 * Factory of MyBatis mappers and services default services.
 */
public class DataRepoFsModule {

  //Guice injector
  private final Injector injector;

  private final DataRepoConfiguration configuration;

  private static final String USERS_DB_CONF_PREFIX = "drupal.db";

  private DoiRegistrationService doiRegistrationService;

  /**
   * Initializes mappers from the configuration settings and environment.
   */
  public DataRepoFsModule(DataRepoConfiguration configuration, MetricRegistry metricRegistry,
                          HealthCheckRegistry healthCheckRegistry) {
    this.configuration = configuration;
    injector = Guice.createInjector(new DataPackageMyBatisModule(configuration.getDbConfig(),
                                                                                     metricRegistry,
                                                                                     healthCheckRegistry));
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
   * Lazy creation of a DoiRegistrationService.
   * If the instance has ben created previously it is re-used.
   */
  public DoiRegistrationService doiRegistrationService(ObjectMapper mapper) {
    if (doiRegistrationService == null) {
      Client client = DoiRegistrationWsClient.buildClient(configuration, mapper);
      doiRegistrationService = new DoiRegistrationWsClient(buildWebTarget(client, configuration.getGbifApiUrl()));
    }
    return doiRegistrationService;
  }

  /**
   * Creates an instance of DataRepository that is backed by a file system.
   */
  public DataRepository dataRepository(ObjectMapper mapper) {
    return new FileSystemRepository(configuration, doiRegistrationService(mapper), dataPackageMapper(),
                                    dataPackageFileMapper(), repositoryStatsMapper(), alternativeIdentifierMapper());
  }

}
