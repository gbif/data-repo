package org.gbif.datarepo.store.fs.conf;

import javax.validation.constraints.NotNull;

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
  public String getAppSecretKey() {
    return appSecretKey;
  }

  public void setAppSecretKey(String appSecretKey) {
    this.appSecretKey = appSecretKey;
  }
}
