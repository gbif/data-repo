package org.gbif.datarepo.inject;

import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.fs.DataRepoFileSystemService;
import org.gbif.datarepo.persistence.DataPackageMyBatisModule;
import org.gbif.datarepo.persistence.DataRepoPersistenceService;
import org.gbif.datarepo.registry.DoiRegistrationWsClient;
import org.gbif.datarepo.impl.FileSystemDataRepository;
import org.gbif.datarepo.impl.conf.DataRepoConfiguration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import javax.ws.rs.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.hadoop.fs.Path;

import static org.gbif.datarepo.registry.DoiRegistrationWsClient.buildWebTarget;

/**
 * Factory of MyBatis mappers and services default services.
 */
public class DataRepoFsModule {

  //Guice injector
  private final Injector injector;

  private final DataRepoConfiguration configuration;

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
   * Lazy creation of a DoiRegistrationService.
   * If the instance has ben created previously it is re-used.
   */
  public DoiRegistrationService doiRegistrationService(ObjectMapper mapper) {
    if (doiRegistrationService == null) {
      Client client = DoiRegistrationWsClient.buildClient(configuration.getAppKey(), mapper);
      doiRegistrationService = new DoiRegistrationWsClient(buildWebTarget(client, configuration.getGbifApiUrl()));
    }
    return doiRegistrationService;
  }

  /**
   * Creates an instance of DataRepository that is backed by a file system.
   */
  public DataRepository dataRepository(ObjectMapper mapper) {
    return new FileSystemDataRepository(doiRegistrationService(mapper),
                                        injector.getInstance(DataRepoPersistenceService.class),
                                        new DataRepoFileSystemService(new Path(configuration.getDataRepoPath()),
                                                                  configuration.getFileSystem()),
                                        configuration.getDataRepoName());
  }

}
