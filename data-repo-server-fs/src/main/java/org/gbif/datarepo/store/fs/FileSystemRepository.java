package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.RepositoryStats;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.persistence.mappers.IdentifierMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;
import org.gbif.datarepo.store.fs.download.FileDownload;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
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

  private final IdentifierMapper identifierMapper;

  private final RepositoryStatsMapper repositoryStatsMapper;

  private final TagMapper tagMapper;


  /**
   * Paths where the files are stored.
   */
  private final Path storePath;


  private final FileSystem fileSystem;

  private final FileDownload fileDownload;

  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(String dataRepoPath,
                              DoiRegistrationService doiRegistrationService,
                              DataPackageMapper dataPackageMapper,
                              DataPackageFileMapper dataPackageFileMapper,
                              TagMapper tagMapper,
                              RepositoryStatsMapper repositoryStatsMapper,
                              IdentifierMapper identifierMapper,
                              FileSystem fileSystem) {
    try {
      this.doiRegistrationService = doiRegistrationService;
      this.fileSystem = fileSystem;
      fileDownload = new FileDownload(fileSystem);
      storePath = new Path(dataRepoPath);

      //Create directory if it doesn't exist
      if (!fileSystem.exists(storePath)) {
        Preconditions.checkState(fileSystem.mkdirs(storePath), "Error creating data directory");
      }
      Preconditions.checkArgument(fileSystem.isDirectory(storePath), "Repository is not a directory");
      this.dataPackageMapper = dataPackageMapper;
      this.dataPackageFileMapper = dataPackageFileMapper;
      this.repositoryStatsMapper = repositoryStatsMapper;
      this.identifierMapper = identifierMapper;
      this.tagMapper = tagMapper;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   * Returns the new path where the file is stored.
   */
  private Path store(UUID dataPackageKey, FileInputContent fileInputContent) {
    try {
      Path doiPath = getPath(dataPackageKey);
      if (!fileSystem.exists(doiPath)) {
        fileSystem.mkdirs(doiPath);
      }
      Path newFilePath = resolve(doiPath, fileInputContent.getName());
      fileDownload.copy(fileInputContent, newFilePath, fileSystem);
      return newFilePath;
    } catch (IOException ex) {
      LOG.error("Error storing file {}", fileInputContent.getName(), ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Removes all files and directories of a data package.
   */
  private void clearDataPackageDir(UUID dataPackageKey) {
    try {
      Path doiDir = getPath(dataPackageKey);
      if (fileSystem.exists(doiDir)) {
        fileSystem.delete(doiDir, true);
        fileSystem.mkdirs(doiDir);
      }
    } catch (IOException ex) {
      LOG.error("Error deleting datapackage content {}", ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Store metadata file.
   */
  private void storeMetadata(UUID dataPackageKey, InputStream file) {
    store(dataPackageKey, FileInputContent.from(DataPackage.METADATA_FILE, file));
  }

  /**
   * Deletes the entire directory and its contents from a DOI.
   */
  @Override
  public void delete(UUID dataPackageKey) {
    DataPackage dataPackage = dataPackageMapper.getByKey(dataPackageKey);
    dataPackageMapper.delete(dataPackageKey);
    Optional.ofNullable(dataPackage.getDoi())
      .ifPresent(doi ->  doiRegistrationService.delete(doi.getPrefix(), doi.getSuffix()));
    try {
      fileSystem.delete(getPath(dataPackageKey), true);
    } catch (IOException ex) {
      LOG.error("Error deleting DataPackage {} directory", dataPackageKey);
    }
  }

  /**
   * Marks the DataPackage as deleted.
   */
  @Override
  public void archive(UUID dataPackageKey) {
    dataPackageMapper.archive(dataPackageKey);
  }

  private DataPackage prePersist(DataPackage dataPackage, List<FileInputContent> newFiles, UUID dataPackageKey) {
    DataPackage newDataPackage = new DataPackage();
    newDataPackage.setDoi(dataPackage.getDoi());
    newDataPackage.setKey(dataPackageKey);
    newDataPackage.setMetadata(DataPackage.METADATA_FILE);
    newDataPackage.setCreatedBy(dataPackage.getCreatedBy());
    newDataPackage.setTitle(dataPackage.getTitle());
    newDataPackage.setDescription(dataPackage.getDescription());
    newDataPackage.setCreated(dataPackage.getCreated());
    newDataPackage.setModified(dataPackage.getModified());

    //store all the submitted files
    newFiles.stream().forEach(fileInputContent -> {
      Path newFilePath = store(dataPackageKey, fileInputContent);
      long fileLength = size(newFilePath);
      DataPackageFile dataPackageFile = new DataPackageFile(newFilePath.getName(),
                                                            md5(newFilePath), fileLength);
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
        alternativeIdentifier.setDataPackageKey(dataPackageKey);
        alternativeIdentifier.setCreatedBy(dataPackage.getCreatedBy());
        newDataPackage.addAlternativeIdentifier(alternativeIdentifier);
      });
    dataPackage.getTags().forEach(tag -> newDataPackage.addTag(tag.getValue()));

    return newDataPackage;
  }

  private static DataCiteMetadata toDataCiteMetadata(DataPackage dataPackage) {
    Date metadataCreationDate = Optional.ofNullable(dataPackage.getCreated()).orElse(new Date());
    return
      DataCiteMetadata.builder()
      .withCreators(DataCiteMetadata.Creators.builder()
                      .withCreator(DataCiteMetadata.Creators.Creator.builder()
                                     .withCreatorName(dataPackage.getCreatedBy())
                                     .build())
                      .build())
      .withTitles(DataCiteMetadata.Titles.builder()
                    .withTitle(DataCiteMetadata.Titles.Title.builder()
                                 .withValue(dataPackage.getTitle())
                                 .build())
                    .build())
      .withDates(DataCiteMetadata.Dates.builder()
                   .withDate(DataCiteMetadata.Dates.Date.builder()
                               .withDateType(DateType.CREATED)
                               .withValue(ISO8601Utils.format(metadataCreationDate))
                               .build())
                   .build())
      .withPublisher(dataPackage.getCreatedBy())
      .withPublicationYear(String.valueOf(metadataCreationDate.toInstant()
                                            .atZone(ZoneId.systemDefault()).getYear()))
      .withDescriptions(DataCiteMetadata.Descriptions.builder()
                          .withDescription(DataCiteMetadata.Descriptions.Description
                                             .builder()
                                             .withContent(dataPackage.getDescription())
                                             .withDescriptionType(DescriptionType.ABSTRACT)
                                             .build())
                          .build())
      .build();
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(DataPackage dataPackage, List<FileInputContent> files) {
    try (InputStream xmlMetadata = new ByteArrayInputStream(DataCiteValidator.toXml(toDataCiteMetadata(dataPackage), false).getBytes())) {
      return create(dataPackage, xmlMetadata, files);
    } catch (InvalidMetadataException | IOException  ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files) {
    UUID dataPackageKey  = UUID.randomUUID();
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
                                                                                        .build())),
                                              dataPackageKey))
            .map(doi -> {
              try {
                DataPackage newDataPackage = prePersist(dataPackage, files, dataPackageKey);
                //Persist data package info
                dataPackageMapper.create(newDataPackage);
                newDataPackage.getFiles()
                  .forEach(dataPackageFile -> dataPackageFileMapper.create(newDataPackage.getKey(), dataPackageFile));
                newDataPackage.getAlternativeIdentifiers().forEach(identifierMapper::create);
                newDataPackage.getTags().forEach(tagMapper::create);
                return newDataPackage;
              } catch (Exception ex) {
                LOG.error("Error registering a DOI", ex);
                //Deletes all data created to this DOI in case from error
                delete(dataPackageKey);
                throw new RuntimeException(ex);
              }
            }).orElseThrow(() -> new IllegalStateException("DataPackage couldn't be created"));
  }

  /**
   * Deletes all files of a DataPackage.
   */
  private void clearDataPackageContent(DataPackage dataPackage) {
    dataPackage.getFiles()
      .forEach(dataPackageFile ->
                 dataPackageFileMapper.delete(dataPackage.getKey(), dataPackageFile.getFileName()));
    clearDataPackageDir(Optional.ofNullable(dataPackage.getKey()).orElseThrow(() -> new RuntimeException("Doi not supplied")));
    dataPackage.getFiles().clear();
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage update(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files,
                            UpdateMode mode) {
    DataPackage existingDataPackage =  get(dataPackage.getKey())
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
      dataPackageFileMapper.delete(existingDataPackage.getKey(), dataPackageFile.getFileName());
      existingDataPackage.getFiles().remove(dataPackageFile);
    });
    existingDataPackage.setModified(dataPackage.getModified());
    existingDataPackage.getAlternativeIdentifiers()
      .forEach(alternativeIdentifier -> identifierMapper.delete(alternativeIdentifier.getKey()));
    existingDataPackage.getTags().forEach(tag -> tagMapper.delete(tag.getKey()));
    DataPackage preparedDataPackage = prePersist(existingDataPackage, files, existingDataPackage.getKey());
    preparedDataPackage.getFiles().stream()
      .filter(dataPackageFile -> existingFiles.get(Boolean.FALSE)
                                  .stream()
                                  .noneMatch(extDp -> extDp.getFileName()
                                                       .equalsIgnoreCase(dataPackageFile.getFileName())))
      .forEach(dataPackageFile -> dataPackageFileMapper.create(existingDataPackage.getKey(), dataPackageFile));

    dataPackageMapper.update(preparedDataPackage);
    preparedDataPackage.getAlternativeIdentifiers().forEach(identifierMapper::create);
    preparedDataPackage.getTags().forEach(tagMapper::create);
    handleMetadata(metadata, dataCiteMetadata -> doiRegistrationService.update(DoiRegistration.builder()
                                                                                  .withType(DoiType.DATA_PACKAGE)
                                                                                  .withMetadata(dataCiteMetadata)
                                                                                  .withUser(preparedDataPackage
                                                                                              .getCreatedBy())
                                                                                  .withDoi(preparedDataPackage
                                                                                             .getDoi())
                                                                                  .build()),
                   existingDataPackage.getKey());
    return preparedDataPackage;
  }

  /**
   * Read, store and a register the supplied metadata.
   */
  private DOI handleMetadata(InputStream metadata, Function<String, DOI> registrationHandler,
                             UUID dataPackageKey) {
    try (ByteArrayInputStream  metadataInputStream = new ByteArrayInputStream(IOUtils.toByteArray(metadata))) {
      metadataInputStream.mark(0);
      String dataCiteMetadata = IOUtils.toString(metadataInputStream);
      DOI doi = registrationHandler.apply(dataCiteMetadata);
      //Store metadata.xml file
      metadataInputStream.reset(); //reset the input stream
      storeMetadata(dataPackageKey, metadataInputStream);
      return doi;
    } catch (IOException ex) {
      LOG.error("Error reading data package metadata", ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Retrieves the DataPackage content stored for the UUID.
   */
  @Override
  public Optional<DataPackage> get(UUID dataPackageKey) {
    return Optional.ofNullable(dataPackageMapper.getByKey(dataPackageKey));
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public Optional<DataPackage> get(DOI doi) {
    return Optional.ofNullable(dataPackageMapper.getByDOI(doi.getDoiName()));
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
                                          @Nullable Boolean deleted, @Nullable List<String> tags,
                                          @Nullable String q) {
    Long count = dataPackageMapper.count(user, fromDate, toDate, deleted, tags, q);
    List<DataPackage> packages = dataPackageMapper.list(user, page, fromDate, toDate, deleted, tags, q);
    return new PagingResponse<>(page, count, packages);
  }


  /**
   * Gets the file, if it exists, stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<DataPackageFile> getFile(UUID dataPackageKey, String fileName) {
    DataPackageFile dataPackageFile =  dataPackageFileMapper.get(dataPackageKey, fileName);
    Path doiPath = getPath(dataPackageKey);
    try {
      if (fileSystem.exists(doiPath) && dataPackageFile != null) {
        Path packageFile =  resolve(doiPath, fileName);
        if (fileSystem.exists(packageFile)) {
          dataPackageFile.setFileName(Path.getPathWithoutSchemeAndAuthority(packageFile).getName());
          return Optional.of(dataPackageFile);
        }
      }
      return Optional.empty();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

  }

  /**
   * Gets the file stream, if exists, from file stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<InputStream> getFileInputStream(UUID dataPackageKey, String fileName) {
    try {
      Optional<DataPackageFile> packageFile = getFile(dataPackageKey, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(fileSystem.open(resolve(getPath(dataPackageKey),packageFile.get().getFileName())));
      } else if (DataPackage.METADATA_FILE.equalsIgnoreCase(fileName)) {
        return Optional.of(fileSystem.open(resolve(getPath(dataPackageKey),DataPackage.METADATA_FILE)));
      }
    } catch (FileNotFoundException ex) {
      LOG.warn("Requested file {} not found", fileName, ex);
    } catch (IOException ex) {
      LOG.error("Error opening file {}", fileName, ex);
      throw new IllegalStateException(ex);
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
  private Path getPath(UUID dataPackageKey) {
    return new Path(storePath.toString() + '/' + dataPackageKey + '/');
  }

  /**
   * Resolves a path for a DOI.
   */
  public static Path resolve(Path path, String extPath) {
    return new Path(path.toString() + '/' + extPath);
  }

  /**
   * Calculates the MD5 hash of File content.
   */
  public static String md5(File file) {
    try {
      return Files.hash(file, Hashing.md5()).toString();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Calculates the MD5 hash of File content.
   */
  public String md5(Path file) {
    try {
      if (fileSystem instanceof RawLocalFileSystem) {
        return md5(new File(file.toUri().getPath()));
      }
      return fileSystem.getFileChecksum(file).toString().split(":")[1];
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Calculates the MD5 hash of File content.
   */
  public long size(Path file) {
    try {
      return fileSystem.getFileStatus(file).getLen();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
