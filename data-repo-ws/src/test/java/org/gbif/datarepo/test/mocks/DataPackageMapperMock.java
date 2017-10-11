package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.datarepo.test.utils.ResourceTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Mock mapper that works on a files instead of a data base.
 */
public class DataPackageMapperMock implements DataPackageMapper {

  private final Path storePath;


  public DataPackageMapperMock(DataRepoConfiguration configuration) {
    storePath = Paths.get(configuration.getDataRepoPath());
  }

  @Override
  public DataPackage getByAlternativeIdentifier(@Param("identifier") String identifier) {
   return get(identifier);
  }

  @Override
  public DataPackage get(@Param("doi") String doiName) {
    String[] doiNameParts = doiName.indexOf('-') > 0 ? doiName.split("-") : doiName.split("\\/");
    DOI doi = new DOI(doiNameParts[0], doiNameParts[1]);
    File doiPath = getDoiPath(doi).toFile();
    if (doiPath.exists()) {
      //Assemble a new DataPackage instance containing all the information
      DataPackage dataPackage = new DataPackage();
      dataPackage.setDoi(doi);
      dataPackage.setMetadata(DataPackage.METADATA_FILE);
      dataPackage.setCreatedBy(ResourceTestUtils.TEST_USER.getName());
      dataPackage.setTitle("Test Title");
      dataPackage.setDescription("Test Description");
      Arrays.stream(doiPath.listFiles(pathname -> !pathname.getName().equals(DataPackage.METADATA_FILE)))
        .forEach(file -> dataPackage.addFile(file.getName(), FileSystemRepository.md5(file), file.length())); //metadata.xml is excluded from the list from files
      dataPackage.setSize(dataPackage.getFiles().stream().mapToLong(DataPackageFile::getSize).sum());
      dataPackage.setChecksum(dataPackage.getFiles().get(0).getChecksum());
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
  public List<DataPackage> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
                                @Nullable Date fromDate, @Nullable Date toDate, Boolean deleted,
                                @Nullable @Param("tags") List<String> tags,
                                @Nullable @Param("query") String q) {
    return Arrays.stream(storePath.toFile().list()).map(this::get).collect(Collectors.toList());
  }

  @Override
  public Long count(@Nullable @Param("user") String user,
                    @Nullable Date fromDate, @Nullable Date toDate, Boolean deleted,
                    @Nullable @Param("tags") List<String> tags,
                    @Nullable @Param("query") String q) {
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

  @Override
  public void archive(@Param("doi") DOI doi) {
    //NOP;
  }
}
