package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;


import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores archives in a files system repository.
 */
public class FileSystemRepository implements DataRepository {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);

  private final DoiRegistrationService doiRegistrationService;

  private final DataPackageMapper dataPackageMapper;


  /**
   * Paths where the files are stored.
   */
  private final Path storePath;


  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(DataRepoConfiguration configuration,
                              DoiRegistrationService doiRegistrationService,
                              DataPackageMapper dataPackageMapper) {
    this.doiRegistrationService = doiRegistrationService;
    storePath = Paths.get(configuration.getDataRepoPath());
    File file = storePath.toFile();
    //Create directory if it doesn't exist
    if (!file.exists()) {
      Preconditions.checkState(file.mkdirs(), "Error creating data directory");
    }
    Preconditions.checkArgument(file.isDirectory(), "Repository is not a directory");
    this.dataPackageMapper = dataPackageMapper;
  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   */
  private void store(DOI doi, FileInputContent fileInputContent) {
    try {
      Path doiPath = getDoiPath(doi);
      if (!doiPath.toFile().exists()) {
        Files.createDirectory(doiPath);
      }
      Files.copy(fileInputContent.getInputStream(),
                 doiPath.resolve(Paths.get(fileInputContent.getName()).getFileName()), //remove path from file name
                 StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      LOG.error("Error storing file {}", fileInputContent.getName());
      throw new RuntimeException(ex);
    }
  }

  /**
   * Store metadata file.
   */
  @Override
  public void storeMetadata(DOI doi, InputStream file) {
    store(doi, FileInputContent.from(DataPackage.METADATA_FILE, file));
  }

  /**
   * Deletes the entire directory and its contents from a DOI.
   */
  @Override
  public void delete(DOI doi) {
    try {
      dataPackageMapper.delete(doi);
      doiRegistrationService.delete(doi.getPrefix(), doi.getSuffix());
      File doiPath = getDoiPath(doi).toFile();
      if (doiPath.exists()) {
        FileUtils.deleteDirectory(doiPath);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(String userName, InputStream metadata, List<FileInputContent> files) {
    //Generates a DataCiteMetadata object for further validation/manipulation

    try (ByteArrayInputStream  metadataInputStream = new ByteArrayInputStream(IOUtils.toByteArray(metadata))) {
      //mark the input steam to the first position
      metadataInputStream.mark(0);
      String dataCiteMetadata = readMetadata(metadataInputStream);
      //Register DOI
      DOI doi = doiRegistrationService.register(DoiRegistration.builder().withType(DoiType.DATA_PACKAGE)
                                                  .withMetadata(dataCiteMetadata).withUser(userName).build());
      //Store metadata.xml file
      metadataInputStream.reset(); //reset the input stream
      storeMetadata(doi, metadataInputStream);
      try {
        DataPackage dataPackage = new DataPackage();
        dataPackage.setDoi(doi);
        dataPackage.setMetadata(DataPackage.METADATA_FILE);
        dataPackage.setCreatedBy(userName);
        //store all the submitted files
        files.stream().forEach(fileInputContent -> {
          store(doi, fileInputContent);
          dataPackage.addFile(Paths.get(fileInputContent.getName()).getFileName().toString());
        });
        //Persist data package info
        dataPackageMapper.create(dataPackage);
        return dataPackage;
      } catch (Exception ex) {
        LOG.error("Error registering a DOI", ex);
        //Deletes all data created to this DOI in case from error
        delete(doi);
        throw new RuntimeException(ex);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Reads, validates and stores the submitted metadata file.
   */
  private static String readMetadata(InputStream metadataFile) {
    try {
      return IOUtils.toString(metadataFile);
    } catch (IOException ex) {
      LOG.error("Error reading data package metadata", ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public Optional<DataPackage> get(DOI doi) {
    return Optional.of(dataPackageMapper.get(doi.getDoiName()));
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public PagingResponse<DataPackage> list(String user, Pageable page) {
    Long count = dataPackageMapper.count(user, page);
    List<DataPackage>  packages = dataPackageMapper.list(user, page);
    return new PagingResponse<>(page, count, packages);
  }

  /**
   * Gets the file, if exists, stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<File> getFile(DOI doi, String fileName) {
    Path doiPath = getDoiPath(doi);
    if (doiPath.toFile().exists()) {
      File packageFile = doiPath.resolve(fileName).toFile();
      if (packageFile.exists()) {
        return Optional.of(packageFile);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the file stream, if exists, from file stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<InputStream> getFileInputStream(DOI doi, String fileName) {

    try {
      Optional<File> packageFile = getFile(doi, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(new FileInputStream(packageFile.get()));
      }
    } catch (FileNotFoundException ex) {
      LOG.warn("Requested file {} not found", fileName, ex);
    }
    return Optional.empty();
  }

  /**
   * Resolves a path for a DOI.
   */
  private Path getDoiPath(DOI doi) {
    return storePath.resolve(doi.getPrefix() + '-' + doi.getSuffix());
  }
}
