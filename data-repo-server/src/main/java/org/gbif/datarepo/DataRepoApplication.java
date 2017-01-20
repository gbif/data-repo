package org.gbif.datarepo;

import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.conf.DataCiteConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.resource.DataRepoResource;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.datarepo.store.hdfs.FileSystemRepository;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.utils.HttpUtil;

import java.net.URI;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataRepoApplication extends Application<DataRepoConfiguration> {

  @Override
  public void run(DataRepoConfiguration configuration, Environment environment) throws Exception {
    DataRepository dataRepository = new FileSystemRepository(configuration.getDataRepoPath());
    DataCiteService dataCiteService = dataCiteService(configuration.getDataCiteConfiguration());
    DataPackagesDoiGenerator doiGenerator = new DataPackagesDoiGenerator(configuration.getDoiCommonPrefix(),
                                                                         dataCiteService);
    environment.jersey().register(new DataRepoResource(dataRepository, dataCiteService, doiGenerator));
  }


  private static DataCiteService dataCiteService(DataCiteConfiguration dataCiteConfiguration) {
    return new DataCiteService(httpClient(dataCiteConfiguration),
                               dataCiteConfiguration(dataCiteConfiguration));
  }

  private static CloseableHttpClient httpClient(DataCiteConfiguration configuration) {
    return HttpUtil.newMultithreadedClient(configuration.getTimeout(), configuration.getThreads(), configuration.getThreads());
  }

  private static ServiceConfig dataCiteConfiguration(DataCiteConfiguration configuration) {
    ServiceConfig serviceConfig = new ServiceConfig(configuration.getUserName(), configuration.getPassword());
    serviceConfig.setApi(URI.create(configuration.getApiUrl()));
    return  serviceConfig;
  }
}
