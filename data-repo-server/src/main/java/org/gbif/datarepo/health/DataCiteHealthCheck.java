package org.gbif.datarepo.health;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.doi.service.datacite.DataCiteService;

import com.codahale.metrics.health.HealthCheck;

/**
 * Checks that the DataCite service is reachable and running.
 */
public class DataCiteHealthCheck extends HealthCheck {

  private final DataCiteService dataCiteService;
  private final DataPackagesDoiGenerator doiGenerator;

  /**
   * Full constructor.
   * @param dataCiteService proxy to DataCite API
   * @param doiGenerator used to generate test DOIs
   */
  public DataCiteHealthCheck(DataCiteService dataCiteService, DataPackagesDoiGenerator doiGenerator) {
    this.dataCiteService = dataCiteService;
    this.doiGenerator = doiGenerator;
  }

  /**
   * Generates a random DOI and tries to resolve it.
   * The result is ignored, it only tests that the DataService works correctly.
   */
  @Override
  protected Result check() throws Exception {
    dataCiteService.resolve(doiGenerator.randomDOI());
    return Result.healthy("DataCiteService is up and running");
  }
}
