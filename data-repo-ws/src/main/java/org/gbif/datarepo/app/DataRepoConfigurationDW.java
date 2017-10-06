package org.gbif.datarepo.app;

import org.gbif.datarepo.auth.jwt.JwtAuthConfiguration;
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
  private JwtAuthConfiguration jwtAuthConfiguration;

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
  public JwtAuthConfiguration getJwtAuthConfiguration() {
    return jwtAuthConfiguration;
  }

  public void setJwtAuthConfiguration(JwtAuthConfiguration jwtAuthConfiguration) {
    this.jwtAuthConfiguration = jwtAuthConfiguration;
  }
}
