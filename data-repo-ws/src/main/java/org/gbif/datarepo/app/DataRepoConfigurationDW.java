package org.gbif.datarepo.app;

import org.gbif.datarepo.auth.jwt.GbifAuthJwtConfiguration;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.discovery.conf.ServiceConfiguration;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;


/**
 * Main service configuration, it contains: DOI settings, required urls and DB settings.
 */
@JsonAutoDetect
public class DataRepoConfigurationDW extends Configuration {

  @JsonProperty
  private DataRepoConfiguration dataRepoConfiguration;

  @Valid
  private ServiceConfiguration service;

  @JsonProperty
  private GbifAuthJwtConfiguration jwtAuthConfiguration;

  public DataRepoConfiguration getDataRepoConfiguration() {
    return dataRepoConfiguration;
  }

  public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
    this.dataRepoConfiguration = dataRepoConfiguration;
  }

  @JsonProperty
  public ServiceConfiguration getService() {
    return service;
  }

  public void setService(ServiceConfiguration service) {
    this.service = service;
  }

  /**
   * JWT authentication configs.
   */
  public GbifAuthJwtConfiguration getJwtAuthConfiguration() {
    return jwtAuthConfiguration;
  }

  public void setJwtAuthConfiguration(GbifAuthJwtConfiguration jwtAuthConfiguration) {
    this.jwtAuthConfiguration = jwtAuthConfiguration;
  }
}
