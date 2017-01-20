package org.gbif.datarepo.conf;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataCiteConfiguration {

  @NotNull
  private String userName;

  @NotNull
  private String password;

  @NotNull
  private String apiUrl;

  private int threads = 1;

  private int timeout = 6000;

  @JsonProperty
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @JsonProperty
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @JsonProperty
  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  @JsonProperty
  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  @JsonProperty
  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
