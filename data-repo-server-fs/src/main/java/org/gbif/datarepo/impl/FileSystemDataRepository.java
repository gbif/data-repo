package org.gbif.datarepo.impl;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.api.model.RepositoryStats;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.citation.CitationGenerator;
import org.gbif.datarepo.fs.DataRepoFileSystemService;
import org.gbif.datarepo.api.validation.identifierschemes.IdentifierSchemaValidatorFactory;
import org.gbif.datarepo.persistence.DataRepoPersistenceService;
import org.gbif.datarepo.impl.metadata.DataCiteMetadataGenerator;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.hash.Hashing;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores archives in a files system repository.
 */
public class FileSystemDataRepository implements DataRepository {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemDataRepository.class);

  private final DoiRegistrationService doiRegistrationService;

  private final DataRepoPersistenceService persistenceService;

  private final DataRepoFileSystemService fileSystemService;

  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemDataRepository(DoiRegistrationService doiRegistrationService,
                                  DataRepoPersistenceService persistenceService,
                                  DataRepoFileSystemService fileSystemService) {
      this.persistenceService = persistenceService;
      this.doiRegistrationService = doiRegistrationService;
      this.fileSystemService = fileSystemService;
  }


  /**
   * Store metadata file.
   */
  private void storeMetadata(UUID dataPackageKey, InputStream file) {
    fileSystemService.store(dataPackageKey, FileInputContent.from(dataPackageKey + ".xml", file));
  }

  /**
   * Deletes the entire directory and its contents from a DOI.
   */
  @Override
  public void delete(UUID key) {
    DataPackage dataPackage = persistenceService.getDataPackage(key);
    persistenceService.deleteDataPackage(key);
    Optional.ofNullable(dataPackage.getDoi())
      .ifPresent(doi ->  doiRegistrationService.delete(doi.getPrefix(), doi.getSuffix()));
    fileSystemService.deleteDataPackage(key);
  }

  /**
   * Marks the DataPackage as deleted.
   */
  @Override
  public void archive(UUID key) {
    persistenceService.archiveDataPackage(key);
  }

  private DataPackage prePersist(DataPackage dataPackage, Collection<FileInputContent> newFiles, UUID dataPackageKey) {
    DataPackage newDataPackage = new DataPackage();
    newDataPackage.setDoi(dataPackage.getDoi());
    newDataPackage.setKey(dataPackageKey);
    newDataPackage.setCreatedBy(dataPackage.getCreatedBy());
    newDataPackage.setTitle(dataPackage.getTitle());
    newDataPackage.setDescription(dataPackage.getDescription());
    newDataPackage.setCreated(dataPackage.getCreated());
    newDataPackage.setModified(dataPackage.getModified());
    newDataPackage.setLicense(dataPackage.getLicense());

    //store all the submitted files
    newFiles.stream().forEach(fileInputContent -> {
      Path newFilePath = fileSystemService.store(dataPackageKey, fileInputContent);
      long fileLength = fileSystemService.fileSize(newFilePath);
      DataPackageFile dataPackageFile = new DataPackageFile(newFilePath.getName(),
                                                            fileSystemService.md5(newFilePath), fileLength);
      newDataPackage.setSize(newDataPackage.getSize() + fileLength);
      newDataPackage.addFile(dataPackageFile);
    });

    if (newDataPackage.getFiles().size() == 1) {
      newDataPackage.setChecksum(newDataPackage.getFiles().iterator().next().getChecksum());
    } else {
      newDataPackage.setChecksum(Hashing.md5().hashBytes(newDataPackage.getFiles().stream()
                                                           .map(DataPackageFile::getChecksum)
                                                           .collect(Collectors.joining())
                                                           .getBytes(Charset.forName("UTF8"))).toString());
    }

    dataPackage.getRelatedIdentifiers().forEach(newDataPackage::addRelatedIdentifier);
    dataPackage.getCreators().forEach(creator -> {
      if (creator.getIdentifierScheme() != null) {
        creator.setSchemeURI(creator.getIdentifierScheme().getSchemeURI());
        creator.setIdentifier(IdentifierSchemaValidatorFactory.getValidator(creator.getIdentifierScheme())
                                .normalize(creator.getIdentifier()));
      }
      newDataPackage.addCreator(creator);
    });
    dataPackage.getTags().forEach(tag -> newDataPackage.addTag(tag.getValue()));

    return newDataPackage;
  }



  /**
   * Utility method to validate if an identifier has been  used as alternative identifier for another data package.
   */
  @Override
  public boolean isAlternativeIdentifierInUse(Identifier alternativeIdentifier) {
    return persistenceService.listIdentifiers(null, null, alternativeIdentifier.getIdentifier(), null, null,
                                              Identifier.RelationType.IsAlternativeOf, null).getCount() > 0;
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(DataPackage dataPackage, List<FileInputContent> files, boolean generateDOI) {
    try (InputStream xmlMetadata = new ByteArrayInputStream(DataCiteMetadataGenerator.toXmlDataCiteMetadata(dataPackage).getBytes())) {
      return create(dataPackage, xmlMetadata, files, generateDOI);
    } catch (InvalidMetadataException | IOException  ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  private DataPackage create(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files,
                             boolean generateDOI) {

    if (dataPackage.getRelatedIdentifiers() != null && dataPackage.getRelatedIdentifiers()
                                                         .stream()
                                                         .filter(this::isAlternativeIdentifierInUse)
                                                         .count() > 0) {
      throw new IllegalStateException("An identifier has been used as alternative identifier in other data package");
    }
    UUID dataPackageKey  = UUID.randomUUID();
    //Generates a DataCiteMetadata object for further validation/manipulation
    try {
      DataPackage newDataPackage = prePersist(dataPackage, files, dataPackageKey);
      newDataPackage.setDoi(handleMetadata(metadata, dataCiteMetadata ->
                                           generateDOI? doiRegistrationService
                                             .register(DoiRegistration.builder()
                                                         .withType(DoiType.DATA_PACKAGE)
                                                         .withMetadata(dataCiteMetadata)
                                                         .withUser(dataPackage.getCreatedBy())
                                                         .withDoi(dataPackage.getDoi()).build()) : null,
                                           dataPackageKey));

      newDataPackage.setCitation(CitationGenerator.generateCitation(newDataPackage));
      //Persist data package info
      return persistenceService.create(newDataPackage);
    } catch (Exception ex) {
      LOG.error("Error registering a DOI", ex);
      //Deletes all data created to this DOI in case from error
      delete(dataPackageKey);
      throw new RuntimeException(ex);
    }
  }


  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  private DataPackage update(DataPackage dataPackage, InputStream metadata, List<FileInputContent> files,
                            UpdateMode mode) {
    if (UpdateMode.OVERWRITE == mode) {
      fileSystemService.clearDataPackageDir(dataPackage.getKey());
    }

    DataPackage preparedDataPackage = prePersist(dataPackage, files, dataPackage.getKey());
    persistenceService.update(dataPackage, mode);
    handleMetadata(metadata, dataCiteMetadata -> preparedDataPackage.getDoi() != null ?
                                                    doiRegistrationService.update(DoiRegistration.builder()
                                                                                  .withType(DoiType.DATA_PACKAGE)
                                                                                  .withMetadata(dataCiteMetadata)
                                                                                  .withUser(preparedDataPackage
                                                                                              .getCreatedBy())
                                                                                  .withDoi(preparedDataPackage
                                                                                             .getDoi())
                                                                                  .build()):null,
                   dataPackage.getKey());
    return preparedDataPackage;
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage update(DataPackage dataPackage, List<FileInputContent> files, UpdateMode mode) {
    try (InputStream xmlMetadata = new ByteArrayInputStream(DataCiteMetadataGenerator
                                                              .toXmlDataCiteMetadata(dataPackage).getBytes())) {
      return update(dataPackage, xmlMetadata, files, mode);
    } catch (InvalidMetadataException | IOException  ex) {
      throw new IllegalStateException(ex);
    }
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
    return Optional.ofNullable(persistenceService.getDataPackage(dataPackageKey));
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public Optional<DataPackage> get(DOI doi) {
    return Optional.ofNullable(persistenceService.getDataPackage(doi));
  }

  /**
   * Retrieves the DataPackage associated to an alternative identifier.
   */
  @Override
  public Optional<DataPackage> getByAlternativeIdentifier(String identifier) {
    return Optional.ofNullable(persistenceService.getDataPackageByAlternativeIdentifier(identifier));
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public PagingResponse<DataPackage> list(String user, @Nullable Pageable page,
                                          @Nullable Date fromDate, @Nullable Date toDate,
                                          @Nullable Boolean deleted, @Nullable List<String> tags,
                                          @Nullable String q) {
    return persistenceService.listDataPackages(user, page, fromDate, toDate, deleted, tags, q);
  }

  /**
   * Page through AlternativeIdentifiers, optionally filtered by user and dates.
   */
  @Override
  public PagingResponse<Identifier> listIdentifiers(@Nullable String user, @Nullable Pageable page,
                                                    @Nullable String identifier,
                                                    @Nullable UUID dataPackageKey,
                                                    @Nullable Identifier.Type type,
                                                    @Nullable Identifier.RelationType relationType,
                                                    @Nullable Date created) {
    return persistenceService.listIdentifiers(user, page, identifier, dataPackageKey, type, relationType, created);
  }


  /**
   * Gets the file, if it exists.
   */
  @Override
  public Optional<DataPackageFile> getFile(UUID dataPackageKey, String fileName) {
    return Optional.ofNullable(persistenceService.getDataPackageFile(dataPackageKey, fileName));
  }

  /**
   * Gets the file stream, if exists, from file stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<InputStream> getFileInputStream(UUID dataPackageKey, String fileName) {
    try {
      Optional<DataPackageFile> packageFile = getFile(dataPackageKey, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(fileSystemService.openDataPackageFile(dataPackageKey, fileName));
      }
    } catch (IOException ex) {
      LOG.error("Error opening file {}", fileName, ex);
      throw new IllegalStateException(ex);
    }
    return Optional.empty();
  }

  @Override
  public RepositoryStats getStats() {
    return persistenceService.getRepositoryStats();
  }

}
