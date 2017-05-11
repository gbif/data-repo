package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
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

  private final DataPackageFileMapper dataPackageFileMapper;


  /**
   * Paths where the files are stored.
   */
  private final Path storePath;


  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(DataRepoConfiguration configuration,
                              DoiRegistrationService doiRegistrationService,
                              DataPackageMapper dataPackageMapper,
                              DataPackageFileMapper dataPackageFileMapper) {
    this.doiRegistrationService = doiRegistrationService;
    storePath = Paths.get(configuration.getDataRepoPath());
    File file = storePath.toFile();
    //Create directory if it doesn't exist
    if (!file.exists()) {
      Preconditions.checkState(file.mkdirs(), "Error creating data directory");
    }
    Preconditions.checkArgument(file.isDirectory(), "Repository is not a directory");
    this.dataPackageMapper = dataPackageMapper;
    this.dataPackageFileMapper = dataPackageFileMapper;
  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   * Returns the new path where the file is stored.
   */
  private Path store(DOI doi, FileInputContent fileInputContent) {
    try {
      Path doiPath = getDoiPath(doi);
      if (!doiPath.toFile().exists()) {
        Files.createDirectory(doiPath);
      }
      Path newFilePath = doiPath.resolve(Paths.get(fileInputContent.getName()).getFileName());
      Files.copy(fileInputContent.getInputStream(),
                 newFilePath, //remove path from file name
                 StandardCopyOption.REPLACE_EXISTING);
      return newFilePath;
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
  public DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files) {
    //Generates a DataCiteMetadata object for further validation/manipulation

    try (ByteArrayInputStream  metadataInputStream = new ByteArrayInputStream(IOUtils.toByteArray(metadata))) {
      //mark the input steam to the first position
      metadataInputStream.mark(0);
      String dataCiteMetadata = readMetadata(metadataInputStream);
      //Register DOI
      DOI doi = doiRegistrationService.register(DoiRegistration.builder().withType(DoiType.DATA_PACKAGE)
                                                  .withMetadata(dataCiteMetadata).withUser(dataPackage.getCreatedBy())
                                                  .build());
      //Store metadata.xml file
      metadataInputStream.reset(); //reset the input stream
      storeMetadata(doi, metadataInputStream);
      try {
        DataPackage newDataPackage = new DataPackage();
        newDataPackage.setDoi(doi);
        newDataPackage.setMetadata(DataPackage.METADATA_FILE);
        newDataPackage.setCreatedBy(dataPackage.getCreatedBy());
        newDataPackage.setTitle(dataPackage.getTitle());
        newDataPackage.setDescription(dataPackage.getDescription());

        //store all the submitted files
        files.stream().forEach(fileInputContent -> {
          Path newFilePath = store(doi, fileInputContent);
          File newFile = newFilePath.toFile();
          long fileLength = newFile.length();
          DataPackageFile dataPackageFile = new DataPackageFile(newFilePath.getFileName().toString(),
                                                                md5(newFile), fileLength);
          newDataPackage.setSize(newDataPackage.getSize() + fileLength);
          newDataPackage.addFile(dataPackageFile);
        });
        if (newDataPackage.getFiles().size() == 1) {
          newDataPackage.setChecksum(newDataPackage.getFiles().get(0).getChecksum());
        } else {
          newDataPackage.setChecksum(Hashing.md5().hashBytes(newDataPackage.getFiles().stream().map(DataPackageFile::getChecksum).collect(Collectors.joining()).getBytes()).toString());
        }
        //Persist data package info
        dataPackageMapper.create(newDataPackage);
        newDataPackage.getFiles().forEach(dataPackageFile -> dataPackageFileMapper.create(doi, dataPackageFile));
        return newDataPackage;
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
  public PagingResponse<DataPackage> list(String user, @Nullable Pageable page,
                                          @Nullable Date fromDate, @Nullable Date toDate) {
    Long count = dataPackageMapper.count(user, page, fromDate, toDate);
    List<DataPackage>  packages = dataPackageMapper.list(user, page, fromDate, toDate);
    return new PagingResponse<>(page, count, packages);
  }


  /**
   * Gets the file, if it exists, stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<DataPackageFile> getFile(DOI doi, String fileName) {
    DataPackageFile dataPackageFile =  dataPackageFileMapper.get(doi, fileName);
    Path doiPath = getDoiPath(doi);
    if (doiPath.toFile().exists() && dataPackageFile != null) {
      File packageFile = doiPath.resolve(fileName).toFile();
      if (packageFile.exists()) {
        dataPackageFile.setFileName(packageFile.getAbsolutePath());
        return Optional.of(dataPackageFile);
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
      Optional<DataPackageFile> packageFile = getFile(doi, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(new FileInputStream(packageFile.get().getFileName()));
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

  /**
   * Calculates the MD5 hash of File content.
   */
  public static String md5(File file) {
    try {
      return com.google.common.io.Files.hash(file, Hashing.md5()).toString();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
