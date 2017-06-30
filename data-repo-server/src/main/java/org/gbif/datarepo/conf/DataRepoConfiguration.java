package org.gbif.datarepo.conf;

import org.gbif.discovery.conf.ServiceConfiguration;

import java.util.Properties;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;


/**
 * Main service configuration, it contains: DOI settings, required urls and DB settings.
 */
public class DataRepoConfiguration extends Configuration {

  /**
   * Operation Mode of this artifact.
   */
  public enum Mode {
    SERVER, LIBRARY;
  }

  public static final String USERS_DB_CONF_PREFIX = "drupal.db";

  @NotNull
  private Mode mode = Mode.SERVER;

  @NotNull
  private String dataRepoPath;

  @NotNull
  private String doiCommonPrefix;

  @NotNull
  private String gbifApiUrl;

  private String dataPackageApiUrl;

  @Nullable
  private DbConfiguration usersDb;

  @NotNull
  private Properties dbConfig;

  @Valid
  private ServiceConfiguration service;

  @NotNull
  private ApplicationKeyConfiguration appKey;

  /**
   * This server can operate as server or in lib mode, in order to make it reusable in other projects.
   */
  @JsonProperty
  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  /**
   * File system path where the archives are being stored.
   */
  @JsonProperty
  public String getDataRepoPath() {
    return dataRepoPath;
  }

  public void setDataRepoPath(String dataRepoPath) {
    this.dataRepoPath = dataRepoPath;
  }

  /**
   * DOI common prefix, a DOI http://doi.org/10.5072/dl.wf82r4 has "10.5072" as its common prefix.
   */
  @JsonProperty
  public String getDoiCommonPrefix() {
    return doiCommonPrefix;
  }

  public void setDoiCommonPrefix(String doiCommonPrefix) {
    this.doiCommonPrefix = doiCommonPrefix;
  }

  /**
   * GBIF API Url, used to construct URLs from data packages elements.
   */
  @JsonProperty
  public String getGbifApiUrl() {
    return gbifApiUrl;
  }

  public void setGbifApiUrl(String gbifApiUrl) {
    this.gbifApiUrl = gbifApiUrl;
  }

  @JsonProperty
  public String getDataPackageApiUrl() {
    return dataPackageApiUrl;
  }

  public void setDataPackageApiUrl(String dataPackageApiUrl) {
    this.dataPackageApiUrl = dataPackageApiUrl;
  }

  /**
   * Configuration to access the GBIF users portal data base.
   */
  @JsonProperty
  public DbConfiguration getUsersDb() {
    return usersDb;
  }
  public void setUsersDb(DbConfiguration usersDb) {
    this.usersDb = usersDb;
  }

  /**
   * Service Discovery configuration.
   */
  @JsonProperty
  public ServiceConfiguration getService() {
    return service;
  }

  public void setService(ServiceConfiguration service) {
    this.service = service;
  }

  @JsonProperty
  public Properties getDbConfig() {
    return dbConfig;
  }

  public void setDbConfig(Properties dbConfig) {
    this.dbConfig = dbConfig;
  }

  /**
   * Application key/secret used to communicate with other GBIF services.
   */
  @JsonProperty
  public ApplicationKeyConfiguration getAppKey() {
    return appKey;
  }

  public void setAppKey(ApplicationKeyConfiguration appKey) {
    this.appKey = appKey;
  }
}
