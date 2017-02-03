package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.datarepo.api.FileInputContent;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.doi.service.datacite.DataCiteValidator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores archives in a files system repository.
 */
public class FileSystemRepository implements DataRepository {

  private static final String METADATA_FILE = "metadata.xml";

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);

  private final DataPackagesDoiGenerator doiGenerator;

  private final DataCiteService dataCiteService;


  private final DataRepoConfiguration configuration;

  /**
   * Paths where the files are stored.
   */
  private final Path storePath;


  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(DataRepoConfiguration configuration, DataPackagesDoiGenerator doiGenerator,
                              DataCiteService dataCiteService) {
    this.configuration = configuration;
    storePath = Paths.get(configuration.getDataRepoPath());
    File file = storePath.toFile();
    //Create directory if it doesn't exist
    if (!file.exists()) {
      file.mkdirs();
    }
    Preconditions.checkArgument(file.isDirectory(), "Repository is not a directory");
    this.doiGenerator = doiGenerator;
    this.dataCiteService = dataCiteService;
  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   */
  @Override
  public void store(DOI doi, FileInputContent fileInputContent) {
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
    store(doi, FileInputContent.of(METADATA_FILE, file));
  }

  /**
   * Deletes the entire directory and its contents of a DOI.
   */
  @Override
  public void delete(DOI doi) {
    try {
      File doiPath = getDoiPath(doi).toFile();
      if (doiPath.exists()) {
        dataCiteService.delete(doi);
        FileUtils.deleteDirectory(doiPath);
      }
    } catch (IOException | DoiException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Creates a new DataPackage containing the metadata and files specified.
   */
  @Override
  public DataPackage create(String userName, InputStream metadata, List<FileInputContent> files) {
    DOI doi = doiGenerator.newDOI();
    //Store metadata.xml file
    storeMetadata(doi, metadata);
    File metadataFile = getFile(doi, METADATA_FILE).get();
    //Generates a DataCiteMetadata object for further validation/manipulation
    DataCiteMetadata dataCiteMetadata = processMetadata(metadataFile, doi);
    try {
      //store all the submitted files
      files.stream().forEach(fileInputContent -> store(doi, fileInputContent));
      //register the new DOI into DataCite
      dataCiteService.register(doi, targetDoiUrl(doi), dataCiteMetadata);
    } catch (DoiException ex) {
      LOG.error("Error registering a DOI", ex);
      //Deletes all data created to this DOI in case of error
      delete(doi);
      throw new RuntimeException(ex);
    }
    return get(doi).get();
  }

  /**
   * Builds a target Url for a DataPackage doi.
   */
  private URI targetDoiUrl(DOI doi) {
    return URI.create(configuration.getGbifPortalUrl() + doi.getDoiName());
  }

  /**
   * Reads, validates and stores the submitted metadata file.
   */
  private DataCiteMetadata processMetadata(File metadataFile, DOI doi) {
    try(InputStream inputStream = new FileInputStream(metadataFile)) {
      DataCiteMetadata dataCiteMetadata = DataCiteValidator.fromXml(inputStream);
      dataCiteMetadata.setIdentifier(DataCiteMetadata.Identifier.builder()
                                       .withValue(doi.getDoiName())
                                       .withIdentifierType(IdentifierType.DOI.name())
                                       .build());
      storeMetadata(doi, new ByteArrayInputStream(DataCiteValidator.toXml(doi, dataCiteMetadata)
                                                                   .getBytes(StandardCharsets.UTF_8)));
      return dataCiteMetadata;
    } catch (JAXBException | IOException | InvalidMetadataException ex) {
      LOG.error("Error reading data package {} metadata", doi, ex);
      delete(doi);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Retrieves the DataPackage content stored for the DOI.
   */
  @Override
  public Optional<DataPackage> get(DOI doi) {
    File doiPath = getDoiPath(doi).toFile();
    if (doiPath.exists()) {
      //Assemble a new DataPackage instance containing all the information
      DataPackage dataPackage = new DataPackage(configuration.getGbifApiUrl() + doi.getSuffix() + '/');
      dataPackage.setDoi(doi.getUrl());
      dataPackage.setMetadata(METADATA_FILE);
      Arrays.stream(doiPath.listFiles(pathname -> !pathname.getName().equals(METADATA_FILE)))
        .forEach(file -> dataPackage.addFile(file.getName())); //metadata.xml is excluded from the list of files
      return Optional.of(dataPackage);
    }
    return Optional.empty();
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
   * Gets the file stream, if exists, of file stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<InputStream> getFileInputStream(DOI doi, String fileName) {

    try {
      Optional<File> packageFile = getFile(doi, fileName);
      if (packageFile.isPresent()) {
        return Optional.of(new FileInputStream(packageFile.get()));
      }
    } catch (FileNotFoundException ex) {
      LOG.warn("Requested file {} not found",fileName, ex);
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
