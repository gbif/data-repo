package org.gbif.datarepo.health;

import org.gbif.datarepo.conf.DataRepoConfiguration;

import java.io.File;
import java.nio.file.Paths;

import com.codahale.metrics.health.HealthCheck;

/**
 * Checks that the data repository directory exists and is readable and writable.
 */
public class DataRepoHealthCheck extends HealthCheck {

  private final DataRepoConfiguration configuration;

  /**
   * Full constructor, requires configuration settings to access the data directory.
   */
  public DataRepoHealthCheck(DataRepoConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Checks that the data directory exists and is readable and writable.
   */
  @Override
  protected Result check() throws Exception {
    File dataRepoDir = Paths.get(configuration.getDataRepoPath()).toFile();

    if (!dataRepoDir.exists()) {
      return Result.unhealthy(String.format("Data directory %s doesn't exists", dataRepoDir.getAbsolutePath()));
    }
    if (!dataRepoDir.canRead()) {
      return Result.unhealthy(String.format("Data directory %s is readable", dataRepoDir.getAbsolutePath()));
    }
    if (!dataRepoDir.canWrite()) {
      return Result.unhealthy(String.format("Data directory %s is writable", dataRepoDir.getAbsolutePath()));
    }
    return Result.healthy("Data directory is healthy");
  }
}
