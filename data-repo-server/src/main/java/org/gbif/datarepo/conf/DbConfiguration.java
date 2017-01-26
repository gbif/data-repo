package org.gbif.datarepo.conf;

import java.util.Properties;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DbConfiguration {


  @NotNull
  private String dataSourceClassName;

  @NotNull
  private String  serverName;

  @NotNull
  private String  databaseName;

  @NotNull
  private String  user;

  @NotNull
  private String  password;

  @NotNull
  private int maximumPoolSize = 3;

  @NotNull
  private int   connectionTimeout = 30000;

  /**
   * Data source fully qualified class name.
   */
  @JsonProperty
  public String getDataSourceClassName() {
    return dataSourceClassName;
  }

  public void setDataSourceClassName(String dataSourceClassName) {
    this.dataSourceClassName = dataSourceClassName;
  }

  /**
   * Database server name.
   */
  @JsonProperty
  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /**
   * Database name.
   */
  @JsonProperty
  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * Database user name.
   */
  @JsonProperty
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Database password.
   */
  @JsonProperty
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Database conenction pool size.
   * @return
   */
  @JsonProperty
  public int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  public void setMaximumPoolSize(int maximumPoolSize) {
    this.maximumPoolSize = maximumPoolSize;
  }

  /**
   * Initial connection time-out.
   */
  @JsonProperty
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  /**
   * Translates this instance into properties object understood by GBIF persistence modules.
   * @param prefix prefix attached to each property key.
   */
  public Properties toProperties(String prefix) {
    Properties properties = new Properties();
    properties.put(prefix + ".dataSourceClassName", dataSourceClassName);
    properties.put(prefix +".dataSource.serverName", serverName);
    properties.put(prefix +".dataSource.databaseName", databaseName);
    properties.put(prefix +".dataSource.user", user);
    properties.put(prefix +".dataSource.password", password);
    properties.put(prefix +".maximumPoolSize", maximumPoolSize);
    properties.put(prefix +".connectionTimeout", connectionTimeout);
    return properties;
  }
}
