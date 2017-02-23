package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public class DataPackageMapperMock implements DataPackageMapper {

  private final Path storePath;

  public DataPackageMapperMock(DataRepoConfiguration configuration) {
    storePath = Paths.get(configuration.getDataRepoPath());
  }

  @Override
  public DataPackage get(@Param("doi") String doiName) {
    String[] doiNameParts = doiName.split("-");
    DOI doi = new DOI(doiNameParts[0], doiNameParts[1]);
    File doiPath = getDoiPath(doi).toFile();
    if (doiPath.exists()) {
      //Assemble a new DataPackage instance containing all the information
      DataPackage dataPackage = new DataPackage();
      dataPackage.setDoi(doi);
      dataPackage.setMetadata(DataPackage.METADATA_FILE);
      Arrays.stream(doiPath.listFiles(pathname -> !pathname.getName().equals(DataPackage.METADATA_FILE)))
        .forEach(file -> dataPackage.addFile(file.getName())); //metadata.xml is excluded from the list of files
      return dataPackage;
    }
    return null;
  }

  /**
   * Resolves a path for a DOI.
   */
  private Path getDoiPath(DOI doi) {
    return storePath.resolve(doi.getPrefix() + '-' + doi.getSuffix());
  }

  @Override
  public List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page) {
    return Arrays.stream(storePath.toFile().list()).map(this::get).collect(Collectors.toList());
  }

  @Override
  public Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page) {
    return Arrays.stream(storePath.toFile().list()).map(this::get).collect(Collectors.counting());
  }

  @Override
  public void create(@Param("dataPackage") DataPackage dataPackage) {
    // NOP
  }

  @Override
  public void update(@Param("dataPackage") DataPackage dataPackage) {
    //NOP
  }

  @Override
  public void delete(@Param("doi") DOI doi) {
    //NOP;
  }
}
