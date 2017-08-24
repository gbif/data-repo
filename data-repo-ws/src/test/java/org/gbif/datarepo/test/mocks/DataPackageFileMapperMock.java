package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mock mapper that works on a files instead of a data base.
 */
public class DataPackageFileMapperMock implements DataPackageFileMapper {

  private final Path storePath;


  public DataPackageFileMapperMock(DataRepoConfiguration configuration) {
    storePath = Paths.get(configuration.getDataRepoPath());
  }

  @Override
  public DataPackageFile get(@Param("doi") DOI doi, @Param("fileName") String fileName) {
    Path doiPath = getDoiPath(doi);
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
  public List<DataPackageFile> list(@Nullable @Param("doi") DOI doi) {
    try {
      Path doiPath = getDoiPath(doi);
      if (doiPath.toFile().exists() && doiPath.toFile().isDirectory()) {
        return Files.list(doiPath)
          .map(path -> {
            File dataFile = path.toFile();
            return new DataPackageFile(path.toString(), FileSystemRepository.md5(dataFile), dataFile.length());
          })
          .collect(Collectors.toList());
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return new ArrayList<>();
  }

  @Override
  public void create(@Param("doi") DOI doi, @Param("dpf") DataPackageFile dataPackageFile) {
    //NOP
  }

  @Override
  public void update(@Param("doi") String doiName, @Param("dpf") DataPackageFile dataPackageFile) {
    //NOP
  }

  @Override
  public void delete(@Param("doi") DOI doi, @Param("fileName") String fileName) {
    // NOP
  }

  @Override
  public void archive(@Param("doi") DOI doi) {
    // NOP
  }

  /**
   * Resolves a path for a DOI.
   */
  private Path getDoiPath(DOI doi) {
    return storePath.resolve(doi.getPrefix() + '-' + doi.getSuffix());
  }
}
