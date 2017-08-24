package org.gbif.datarepo.test.mocks;

import org.gbif.datarepo.api.model.RepositoryStats;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mock mapper that works on a files instead of a data base.
 */
public class RepositoryStatsMapperMock implements RepositoryStatsMapper {

  private final Path storePath;


  public RepositoryStatsMapperMock(DataRepoConfiguration configuration) {
    storePath = Paths.get(configuration.getDataRepoPath());
  }

  @Override
  public RepositoryStats get() {
    RepositoryStats repositoryStats = new RepositoryStats();
    repositoryStats.setNumOfFiles(countFilesInDirectory(storePath.toFile()));
    repositoryStats.setTotalSize(storePath.toFile().getTotalSpace());
    repositoryStats.setAverageFileSize(repositoryStats.getTotalSize() / repositoryStats.getNumOfFiles());
    return repositoryStats;
  }

  /**
   *
   * Counts all files in a directory recursively.
   */
  private static int countFilesInDirectory(File directory) {
    int count = 0;
    for (File file : directory.listFiles()) {
      if (file.isFile()) {
        count++;
      }
      if (file.isDirectory()) {
        count += countFilesInDirectory(file);
      }
    }
    return count;
  }



}
