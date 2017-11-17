package org.gbif.datarepo.impl.conf;

import java.io.IOException;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.RawLocalFileSystem;

/**
 * Main service configuration, it contains: DOI settings, required urls and DB settings.
 */
public class DataRepoConfiguration  {

  @NotNull
  private String dataRepoPath;

  @Nullable
  private String hdfsNameNode;

  @NotNull
  private String doiCommonPrefix;

  @NotNull
  private String gbifApiUrl;

  private String dataPackageApiUrl;

  @Nullable
  private Properties usersDb;

  @NotNull
  private Properties dbConfig;

  @NotNull
  private ApplicationKeyConfiguration appKey;

  /**
   * File system path where the archives are being stored.
   */
  public String getDataRepoPath() {
    return dataRepoPath;
  }

  public void setDataRepoPath(String dataRepoPath) {
    this.dataRepoPath = dataRepoPath;
  }

  /**
   * DOI common prefix, a DOI http://doi.org/10.5072/dl.wf82r4 has "10.5072" as its common prefix.
   */
  public String getDoiCommonPrefix() {
    return doiCommonPrefix;
  }

  public void setDoiCommonPrefix(String doiCommonPrefix) {
    this.doiCommonPrefix = doiCommonPrefix;
  }

  /**
   * GBIF API Url, used to construct URLs from data packages elements.
   */
  public String getGbifApiUrl() {
    return gbifApiUrl;
  }

  public void setGbifApiUrl(String gbifApiUrl) {
    this.gbifApiUrl = gbifApiUrl;
  }

  public String getDataPackageApiUrl() {
    return dataPackageApiUrl;
  }

  public void setDataPackageApiUrl(String dataPackageApiUrl) {
    this.dataPackageApiUrl = dataPackageApiUrl;
  }

  /**
   * Configuration to access the GBIF users portal data base.
   */
  public Properties getUsersDb() {
    return usersDb;
  }

  public void setUsersDb(Properties usersDb) {
    this.usersDb = usersDb;
  }

  public Properties getDbConfig() {
    return dbConfig;
  }

  public void setDbConfig(Properties dbConfig) {
    this.dbConfig = dbConfig;
  }

  /**
   * Application key/secret used to communicate with other GBIF services.
   */
  public ApplicationKeyConfiguration getAppKey() {
    return appKey;
  }

  public void setAppKey(ApplicationKeyConfiguration appKey) {
    this.appKey = appKey;
  }

  @Nullable
  public String getHdfsNameNode() {
    return hdfsNameNode;
  }

  public void setHdfsNameNode(@Nullable String hdfsNameNode) {
    this.hdfsNameNode = hdfsNameNode;
  }

  public FileSystem getFileSystem() {
    try {
      Configuration configuration = new Configuration();
      if (hdfsNameNode != null) {
        configuration.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, hdfsNameNode);
        return FileSystem.get(configuration);
      }
      RawLocalFileSystem localFileSystem = new RawLocalFileSystem();
      localFileSystem.initialize(localFileSystem.getUri(), configuration);
      return localFileSystem;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

  }
}
