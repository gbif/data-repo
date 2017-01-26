package org.gbif.datarepo.conf;

import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

/**
 * Main service configuration, it contains: DOI settings, required urls and DB settings.
 */
public class DataRepoConfiguration extends Configuration {

  @NotNull
  private String dataRepoPath;

  @NotNull
  private String doiCommonPrefix;

  @NotNull
  private String doiShoulder;

  @NotNull
  private String gbifPortalUrl;

  @NotNull
  private String gbifApiUrl;

  @NotNull
  private DataCiteConfiguration dataCiteConfiguration;

  @NotNull
  private DbConfiguration usersDb;

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
   * Refers to the portion of a DOI suffix that is not unique and randomly generated.
   * For example: http://doi.org/10.5072/dl.wf82r4 has "10.5072" has as a shoulder "dl.".
   */
  @JsonProperty
  public String getDoiShoulder() {
    return doiShoulder;
  }

  public void setDoiShoulder(String doiShoulder) {
    this.doiShoulder = doiShoulder;
  }

  /**
   * GBIF Portal URL, used to construct the target URL of DOIs.
   * @return
   */
  @JsonProperty
  public String getGbifPortalUrl() {
    return gbifPortalUrl;
  }

  public void setGbifPortalUrl(String gbifPortalUrl) {
    this.gbifPortalUrl = gbifPortalUrl;
  }

  /**
   * GBIF API Url, used to construct URLs of data packages elements.
   */
  @JsonProperty
  public String getGbifApiUrl() {
    return gbifApiUrl;
  }

  public void setGbifApiUrl(String gbifApiUrl) {
    this.gbifApiUrl = gbifApiUrl;
  }

  /**
   * DataCite general configuration.
   * @return
   */
  @JsonProperty
  public DataCiteConfiguration getDataCiteConfiguration() {
    return dataCiteConfiguration;
  }

  public void setDataCiteConfiguration(DataCiteConfiguration dataCiteConfiguration) {
    this.dataCiteConfiguration = dataCiteConfiguration;
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

}
