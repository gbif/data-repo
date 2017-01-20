package org.gbif.datarepo.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class DataRepoConfiguration extends Configuration {

  @NotEmpty
  private String dataRepoPath;

  @NotEmpty
  private String doiCommonPrefix;

  @NotEmpty
  private String gbifPortalUrl;

  @NotEmpty
  private String gbifApiUrl;

  @NotEmpty
  private DataCiteConfiguration dataCiteConfiguration;

  @JsonProperty
  public String getDataRepoPath() {
    return dataRepoPath;
  }

  public void setDataRepoPath(String dataRepoPath) {
    this.dataRepoPath = dataRepoPath;
  }

  @JsonProperty
  public String getDoiCommonPrefix() {
    return doiCommonPrefix;
  }

  public void setDoiCommonPrefix(String doiCommonPrefix) {
    this.doiCommonPrefix = doiCommonPrefix;
  }

  @JsonProperty
  public String getGbifPortalUrl() {
    return gbifPortalUrl;
  }

  public void setGbifPortalUrl(String gbifPortalUrl) {
    this.gbifPortalUrl = gbifPortalUrl;
  }

  @JsonProperty
  public String getGbifApiUrl() {
    return gbifApiUrl;
  }

  public void setGbifApiUrl(String gbifApiUrl) {
    this.gbifApiUrl = gbifApiUrl;
  }

  public DataCiteConfiguration getDataCiteConfiguration() {
    return dataCiteConfiguration;
  }

  @JsonProperty
  public void setDataCiteConfiguration(DataCiteConfiguration dataCiteConfiguration) {
    this.dataCiteConfiguration = dataCiteConfiguration;
  }
}
