package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.RepositoryStats;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.persistence.mappers.AlternativeIdentifierMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

  private final AlternativeIdentifierMapper alternativeIdentifierMapper;

  private final RepositoryStatsMapper repositoryStatsMapper;

  private final TagMapper tagMapper;


  /**
   * Paths where the files are stored.
   */
  private final Path storePath;


  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(String dataRepoPath,
                              DoiRegistrationService doiRegistrationService,
                              DataPackageMapper dataPackageMapper,
                              DataPackageFileMapper dataPackageFileMapper,
                              TagMapper tagMapper,
                              RepositoryStatsMapper repositoryStatsMapper,
                              AlternativeIdentifierMapper alternativeIdentifierMapper) {
    this.doiRegistrationService = doiRegistrationService;
    storePath = Paths.get(dataRepoPath);
    File file = storePath.toFile();
    //Create directory if it doesn't exist
    if (!file.exists()) {
      Preconditions.checkState(file.mkdirs(), "Error creating data directory");
    }
    Preconditions.checkArgument(file.isDirectory(), "Repository is not a directory");
    this.dataPackageMapper = dataPackageMapper;
    this.dataPackageFileMapper = dataPackageFileMapper;
    this.repositoryStatsMapper = repositoryStatsMapper;
    this.alternativeIdentifierMapper = alternativeIdentifierMapper;
    this.tagMapper = tagMapper;
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
      LOG.error("Error storing file {}", fileInputContent.getName(), ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Removes all files and directories of a data package.
   */
  private void clearDOIDir(DOI doi) {
    try {
      File doiDir = getDoiPath(doi).toFile();
      if (doiDir.exists()) {
        FileUtils.cleanDirectory(doiDir);
      }
    } catch (IOException ex) {
      LOG.error("Error deleting datapackage content {}", ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Store metadata file.
   */
  private void storeMetadata(DOI doi, InputStream file) {
    store(doi, FileInputContent.from(DataPackage.METADATA_FILE, file));
  }

  /**
   * Deletes the entire directory and its contents from a DOI.
   */
  @Override
  public void delete(DOI doi) {
    dataPackageMapper.delete(doi);
    doiRegistrationService.delete(doi.getPrefix(), doi.getSuffix());
    try {
      FileUtils.deleteDirectory(getDoiPath(doi).toFile());
    } catch (IOException ex) {
      LOG.error("Error deleting DOI {} directory", doi);
    }
  }

  /**
   * Marks the DataPackage as deleted.
   */
  @Override
  public void archive(DOI doi) {
    dataPackageMapper.archive(doi);
  }

  private DataPackage prePersist(DataPackage dataPackage, List<FileInputContent> newFiles, DOI doi) {
    DataPackage newDataPackage = new DataPackage();
    newDataPackage.setDoi(doi);
    newDataPackage.setMetadata(DataPackage.METADATA_FILE);
    newDataPackage.setCreatedBy(dataPackage.getCreatedBy());
    newDataPackage.setTitle(dataPackage.getTitle());
    newDataPackage.setDescription(dataPackage.getDescription());
    newDataPackage.setCreated(dataPackage.getCreated());
    newDataPackage.setModified(dataPackage.getModified());

    //store all the submitted files
    newFiles.stream().forEach(fileInputContent -> {
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
      newDataPackage.setChecksum(Hashing.md5().hashBytes(newDataPackage.getFiles().stream()
                                                           .map(DataPackageFile::getChecksum)
                                                           .collect(Collectors.joining())
                                                           .getBytes(Charset.forName("UTF8"))).toString());
    }

    dataPackage.getAlternativeIdentifiers()
      .forEach(alternativeIdentifier -> {
        alternativeIdentifier.setDataPackageDoi(doi);
        alternativeIdentifier.setCreatedBy(dataPackage.getCreatedBy());
        newDataPackage.addAlternativeIdentifier(alternativeIdentifier);
      });
    dataPackage.getTags().forEach(tag -> newDataPackage.addTag(tag.getValue()));

    return newDataPackage;
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files) {
    //Generates a DataCiteMetadata object for further validation/manipulation
    return Optional.ofNullable(handleMetadata(metadata, dataCiteMetadata ->
                                                          Optional.ofNullable(dataPackage.getDoi())
                                                            .orElseGet(() -> doiRegistrationService
                                                                              .register(DoiRegistration.builder()
                                                                                          .withType(DoiType
                                                                                                      .DATA_PACKAGE)
                                                                                        .withMetadata(dataCiteMetadata)
                                                                                        .withUser(dataPackage
                                                                                                    .getCreatedBy())
                                                                                        .withDoi(dataPackage.getDoi())
                                                                                        .build()))))
            .map(doi -> {
              try {
                DataPackage newDataPackage = prePersist(dataPackage, files, doi);
                //Persist data package info
                dataPackageMapper.create(newDataPackage);
                newDataPackage.getFiles()
                  .forEach(dataPackageFile -> dataPackageFileMapper.create(doi, dataPackageFile));
                newDataPackage.getAlternativeIdentifiers().forEach(alternativeIdentifierMapper::create);
                newDataPackage.getTags().forEach(tagMapper::create);
                return newDataPackage;
              } catch (Exception ex) {
                LOG.error("Error registering a DOI", ex);
                //Deletes all data created to this DOI in case from error
                delete(doi);
                throw new RuntimeException(ex);
              }
            }).orElseThrow(() -> new IllegalStateException("DataPackage couldn't be created"));
  }

  /**
   * Deletes all files of a DataPackage.
   */
  private void clearDataPackageContent(DataPackage dataPackage) {
    dataPackage.getFiles()
      .forEach(dataPackageFile -> {
        dataPackageFileMapper.delete(dataPackage.getDoi(), dataPackageFile.getFileName());
      });
    clearDOIDir(Optional.ofNullable(dataPackage.getDoi()).orElseThrow(() -> new RuntimeException("Doi not supplied")));
    dataPackage.getFiles().clear();
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage update(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files,
                            UpdateMode mode) {

    DataPackage existingDataPackage =  get(dataPackage.getDoi())
                                        .orElseThrow(() -> new RuntimeException("DataPackage not found"));
    if (UpdateMode.OVERWRITE == mode) {
      clearDataPackageContent(dataPackage);
    }

    //Updates basic information
    existingDataPackage.setDescription(dataPackage.getDescription());
    existingDataPackage.setTitle(dataPackage.getTitle());

    Map<Boolean, List<DataPackageFile>> existingFiles =  existingDataPackage.getFiles().stream()
                                                          .collect(
                                                            Collectors.partitioningBy(
                                                              dataPackageFile -> files.stream()
                                                                .anyMatch(fileInputContent -> fileInputContent.getName()
                                                                  .equalsIgnoreCase(dataPackageFile.getFileName())),
                                                              Collectors.toList()
                                                            ));
    existingFiles.get(Boolean.TRUE).forEach(dataPackageFile -> {
      dataPackageFileMapper.delete(dataPackage.getDoi(), dataPackageFile.getFileName());
      existingDataPackage.getFiles().remove(dataPackageFile);
    });
    existingDataPackage.setModified(dataPackage.getModified());
    existingDataPackage.getAlternativeIdentifiers()
      .forEach(alternativeIdentifier -> alternativeIdentifierMapper.delete(alternativeIdentifier.getIdentifier()));
    existingDataPackage.getTags().forEach(tag -> tagMapper.delete(tag.getKey()));
    DataPackage preparedDataPackage = prePersist(existingDataPackage, files, existingDataPackage.getDoi());
    preparedDataPackage.getFiles().stream()
      .filter(dataPackageFile -> existingFiles.get(Boolean.FALSE)
                                  .stream()
                                  .noneMatch(extDp -> extDp.getFileName()
                                                       .equalsIgnoreCase(dataPackageFile.getFileName())))
      .forEach(dataPackageFile -> dataPackageFileMapper.create(existingDataPackage.getDoi(), dataPackageFile));

    dataPackageMapper.update(preparedDataPackage);
    preparedDataPackage.getAlternativeIdentifiers().forEach(alternativeIdentifierMapper::create);
    preparedDataPackage.getTags().forEach(tagMapper::create);
    handleMetadata(metadata, dataCiteMetadata -> doiRegistrationService.update(DoiRegistration.builder()
                                                                                  .withType(DoiType.DATA_PACKAGE)
                                                                                  .withMetadata(dataCiteMetadata)
                                                                                  .withUser(preparedDataPackage
                                                                                              .getCreatedBy())
                                                                                  .withDoi(preparedDataPackage
                                                                                             .getDoi())
                                                                                  .build()));
    return preparedDataPackage;
  }

  /**
   * Read, store and a register the supplied metadata.
   */
  private DOI handleMetadata(InputStream metadata, Function<String, DOI> registrationHandler) {
    try (ByteArrayInputStream  metadataInputStream = new ByteArrayInputStream(IOUtils.toByteArray(metadata))) {
      metadataInputStream.mark(0);
      String dataCiteMetadata = IOUtils.toString(metadataInputStream);
      DOI doi = registrationHandler.apply(dataCiteMetadata);
      //Store metadata.xml file
      metadataInputStream.reset(); //reset the input stream
      storeMetadata(doi, metadataInputStream);
      return doi;
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
    return Optional.ofNullable(dataPackageMapper.get(doi.getDoiName()));
  }

  /**
   * Retrieves the DataPackage associated to an alternative identifier.
   */
  @Override
  public Optional<DataPackage> getByAlternativeIdentifier(String identifier) {
    return Optional.ofNullable(dataPackageMapper.getByAlternativeIdentifier(identifier));
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public PagingResponse<DataPackage> list(String user, @Nullable Pageable page,
                                          @Nullable Date fromDate, @Nullable Date toDate,
                                          @Nullable Boolean deleted, @Nullable List<String> tags) {
    Long count = dataPackageMapper.count(user, fromDate, toDate, deleted, tags);
    List<DataPackage> packages = dataPackageMapper.list(user, page, fromDate, toDate, deleted, tags);
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
      } else if (DataPackage.METADATA_FILE.equalsIgnoreCase(fileName)) {
        return Optional.of(new FileInputStream(getDoiPath(doi).resolve(DataPackage.METADATA_FILE).toFile()));
      }
    } catch (FileNotFoundException ex) {
      LOG.warn("Requested file {} not found", fileName, ex);
    }
    return Optional.empty();
  }

  @Override
  public RepositoryStats getStats() {
    return repositoryStatsMapper.get();
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
