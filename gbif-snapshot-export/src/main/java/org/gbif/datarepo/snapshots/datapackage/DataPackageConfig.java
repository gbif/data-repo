package org.gbif.datarepo.snapshots.datapackage;

import org.gbif.datarepo.impl.conf.DataRepoConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuration for the package datapackage.
 */
public class DataPackageConfig {

  List<UUID> packageKeys = new ArrayList<>();
  private DataRepoConfiguration dataRepoConfiguration;

  public List<UUID> getPackageKeys() {
    return packageKeys;
  }

  public void setPackageKeys(List<UUID> packageKeys) {
    this.packageKeys = packageKeys;
  }

  public DataRepoConfiguration getDataRepoConfiguration() {
    return dataRepoConfiguration;
  }

  public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
    this.dataRepoConfiguration = dataRepoConfiguration;
  }
}
