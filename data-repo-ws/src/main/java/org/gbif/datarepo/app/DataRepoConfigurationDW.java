package org.gbif.datarepo.app;

import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.discovery.conf.ServiceConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

  @NotNull
  private String jwtSigningKey;

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
   * Jason Web Token used to trust in externally authenticated users.
   */
  public String getJwtSigningKey() {
    return jwtSigningKey;
  }

  public void setJwtSigningKey(String jwtSigningKey) {
    this.jwtSigningKey = jwtSigningKey;
  }
}
