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
import org.gbif.datarepo.store.fs.download.FileDownload;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.MD5MD5CRC32CastagnoliFileChecksum;
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

  private final AlternativeIdentifierMapper alternativeIdentifierMapper;

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
                              AlternativeIdentifierMapper alternativeIdentifierMapper,
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
      this.alternativeIdentifierMapper = alternativeIdentifierMapper;
      this.tagMapper = tagMapper;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   * Returns the new path where the file is stored.
   */
  private Path store(DOI doi, FileInputContent fileInputContent) {
    try {
      Path doiPath = getDoiPath(doi);
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
  private void clearDOIDir(DOI doi) {
    try {
      Path doiDir = getDoiPath(doi);
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
      fileSystem.delete(getDoiPath(doi), true);
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
      long fileLength = size(newFilePath);
      DataPackageFile dataPackageFile = new DataPackageFile(newFilePath.getName().toString(),
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
      .forEach(dataPackageFile ->
                 dataPackageFileMapper.delete(dataPackage.getDoi(), dataPackageFile.getFileName()));
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
  public Optional<DataPackageFile> getFile(DOI doi, String fileName) {
    DataPackageFile dataPackageFile =  dataPackageFileMapper.get(doi, fileName);
    Path doiPath = getDoiPath(doi);
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
  public Optional<InputStream> getFileInputStream(DOI doi, String fileName) {
    try {
      Optional<DataPackageFile> packageFile = getFile(doi, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(fileSystem.open(resolve(getDoiPath(doi),packageFile.get().getFileName())));
      } else if (DataPackage.METADATA_FILE.equalsIgnoreCase(fileName)) {
        return Optional.of(fileSystem.open(resolve(getDoiPath(doi),DataPackage.METADATA_FILE)));
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
  private Path getDoiPath(DOI doi) {
    return new Path(storePath.toString() + '/' + doi.getPrefix() + '-' + doi.getSuffix() + '/');
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
