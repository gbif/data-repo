package org.gbif.datarepo.api.model;

/**
 * General statistics of the repository.
 */
public class RepositoryStats {

  private Integer numOfFiles;

  private Long totalSize;

  private Long averageFileSize;

  /**
   * Number of active files.
   */
  public Integer getNumOfFiles() {
    return numOfFiles;
  }

  public void setNumOfFiles(Integer numOfFiles) {
    this.numOfFiles = numOfFiles;
  }

  /**
   * Total size in bytes: sum of all file size in the repo.
   */
  public Long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(Long totalSize) {
    this.totalSize = totalSize;
  }

  /**
   * Average file size in the repository.
   */
  public Long getAverageFileSize() {
    return averageFileSize;
  }

  public void setAverageFileSize(Long averageFileSize) {
    this.averageFileSize = averageFileSize;
  }
}
