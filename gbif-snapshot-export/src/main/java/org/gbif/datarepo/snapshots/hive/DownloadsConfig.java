package org.gbif.datarepo.snapshots.hive;

import org.gbif.datarepo.impl.conf.DataRepoConfiguration;

import java.util.List;

/**
 * Configuration settings to export a GBIF download into DataOne.
 */
public class DownloadsConfig {

  private DataRepoConfiguration dataRepoConfiguration;
  private List<DownloadInfo> downloads;

  public DataRepoConfiguration getDataRepoConfiguration() {
    return dataRepoConfiguration;
  }

  public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
    this.dataRepoConfiguration = dataRepoConfiguration;
  }

  public List<DownloadInfo> getDownloads() {
    return downloads;
  }

  public void setDownloads(List<DownloadInfo> downloads) {
    this.downloads = downloads;
  }

  enum Format {
    DWCA, CSV, AVRO
  }

  static class DownloadInfo {

    Format format;
    String path;
    String doi;
    long totalRecords;
    String date;

    public Format getFormat() {
      return format;
    }

    public void setFormat(Format format) {
      this.format = format;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getDoi() {
      return doi;
    }

    public void setDoi(String doi) {
      this.doi = doi;
    }

    public long getTotalRecords() {
      return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
      this.totalRecords = totalRecords;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }
  }
}
