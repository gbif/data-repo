package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mock mapper that works on a files instead of a data base.
 */
public class DataPackageFileMapperMock implements DataPackageFileMapper {

  private final Path storePath;


  public DataPackageFileMapperMock(DataRepoConfiguration configuration) {
    storePath = Paths.get(URI.create(configuration.getDataRepoPath()).getPath());
  }

  @Override
  public DataPackageFile get(@Param("dataPackageKey") UUID dataPackageKey, @Param("fileName") String fileName) {
    Path doiPath = getDataPackagePath(dataPackageKey);
    if (doiPath.toFile().exists()) {
      File packageFile = doiPath.resolve(fileName).toFile();
      if (packageFile.exists()) {
        return new DataPackageFile(packageFile.getAbsolutePath(), FileSystemRepository.md5(packageFile),
                                   packageFile.length());
      }
    }
    return null;
  }

  @Override
  public List<DataPackageFile> list(@Nullable @Param("dataPackageKey") UUID dataPackageKey) {
    try {
      Path doiPath = getDataPackagePath(dataPackageKey);
      if (doiPath.toFile().exists() && doiPath.toFile().isDirectory()) {
        return Files.list(doiPath)
          .map(path -> {
            File dataFile = path.toFile();
            return new DataPackageFile(path.toString(), FileSystemRepository.md5(dataFile),
                                       dataFile.length());
          })
          .collect(Collectors.toList());
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return new ArrayList<>();
  }

  @Override
  public void create(@Param("dataPackageKey") UUID dataPackageKey, @Param("dpf") DataPackageFile dataPackageFile) {
    //NOP
  }

  @Override
  public void update(@Param("dataPackageKey") UUID dataPackageKey, @Param("dpf") DataPackageFile dataPackageFile) {
    //NOP
  }

  @Override
  public void delete(@Param("dataPackageKey") UUID dataPackageKey, @Param("fileName") String fileName) {
    // NOP
  }

  @Override
  public void archive(@Param("dataPackageKey") UUID dataPackageKey) {
    // NOP
  }

  /**
   * Resolves a path for a UUID.
   */
  private Path getDataPackagePath(UUID dataPackageKey) {
    return storePath.resolve(dataPackageKey.toString());
  }
}
