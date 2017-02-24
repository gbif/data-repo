package org.gbif.datarepo.conf;

import java.util.Properties;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Database configuration. It is used for compatibility from how data sorces are specified in other GBIF libraries.
 */
public class DbConfiguration {

  //Default connection time-out
  private static final int DEFAULT_TO = 30000;

  //Default connections pool size
  private static final int DEFAULT_POOL_SIZE = 3;

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
  private int maximumPoolSize = DEFAULT_POOL_SIZE;

  @NotNull
  private int   connectionTimeout = DEFAULT_TO;

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
   * Database connection pool size.
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
    properties.setProperty(prefix + ".dataSourceClassName", dataSourceClassName);
    properties.setProperty(prefix + ".dataSource.serverName", serverName);
    properties.setProperty(prefix + ".dataSource.databaseName", databaseName);
    properties.setProperty(prefix + ".dataSource.user", user);
    properties.setProperty(prefix + ".dataSource.password", password);
    properties.setProperty(prefix + ".maximumPoolSize", Integer.toString(maximumPoolSize));
    properties.setProperty(prefix + ".connectionTimeout", Integer.toString(connectionTimeout));
    return properties;
  }
}
