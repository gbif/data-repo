package org.gbif.datarepo.conf;

import org.gbif.doi.service.ServiceConfig;

import java.net.URI;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration settings to access DataCite DOI services.
 */
public class DataCiteConfiguration {

  @NotNull
  private String userName;

  @NotNull
  private String password;

  @NotNull
  private String apiUrl = "https://mds.datacite.org/";

  private int threads = 1;

  private int timeout = 6000;

  /**
   * DataCite user name.
   */
  @JsonProperty
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * DataCite password.
   */
  @JsonProperty
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Url to DataCire API. It has a default value of https://mds.datacite.org/.
   */
  @JsonProperty
  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  /**
   * Number of threads to handle HTTP client requests.
   */
  @JsonProperty
  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  /**
   * Connection tim-out to DataCite API.
   */
  @JsonProperty
  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * Converts this instance into a ServiceConfig instances, which is the class used to create DataCiteService instances.
   */
  public ServiceConfig asServiceConfiguration() {
    ServiceConfig serviceConfig = new ServiceConfig(userName, password);
    serviceConfig.setApi(URI.create(apiUrl));
    return  serviceConfig;
  }
}
