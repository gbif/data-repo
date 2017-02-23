package org.gbif.datarepo.conf;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration to use a external service based on application keys and secrets keys.
 */
public class ApplicationKeyConfiguration {

  @NotNull
  private String appKey;

  @NotNull
  private String appSecretKey;

  /**
   * Application Key/Username.
   */
  public String getAppKey() {
    return appKey;
  }

  public void setAppKey(String appKey) {
    this.appKey = appKey;
  }

  /**
   * Application secret/password.
   */
  @JsonProperty
  public String getAppSecretKey() {
    return appSecretKey;
  }

  @JsonProperty
  public void setAppSecretKey(String appSecretKey) {
    this.appSecretKey = appSecretKey;
  }
}
