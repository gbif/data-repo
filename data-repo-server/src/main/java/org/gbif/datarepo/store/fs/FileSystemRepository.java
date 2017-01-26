package org.gbif.datarepo.store.fs;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores archives in a files system repository.
 */
public class FileSystemRepository implements DataRepository {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);

  /**
   * Paths where the files are stored.
   */
  private final Path storePath;

  /**
   * Default constructor: requires a path to an existing directory.
   */
  public FileSystemRepository(String storePath) {
    this.storePath = Paths.get(storePath);
    File file = this.storePath.toFile();
    //Create directory if it doesn't exist
    if (!file.exists()) {
      file.mkdirs();
    }
    Preconditions.checkArgument(file.isDirectory(), "Repository is not a directory");
  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   */
  @Override
  public void store(DOI doi, InputStream file, String fileName) {
    try {
      Path doiPath = getDoiPath(doi);
      if (!doiPath.toFile().exists()) {
        Files.createDirectory(doiPath);
      }
      Files.copy(file, doiPath.resolve(fileName));
    } catch (IOException ex) {
      LOG.error("Error storing file {}", fileName);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Store metadata file.
   */
  @Override
  public void storeMetadata(DOI doi, InputStream file) {
    store(doi, file, "metadata.xml");
  }

  /**
   * Deletes the entire directory and its contents of a DOI.
   */
  @Override
  public void delete(DOI doi) {
    Optional<Path> doiPath = get(doi);
    if (doiPath.isPresent()) {
      FileUtils.deleteDirectoryRecursively(doiPath.get().toFile());
    }
  }

  /**
   * Retrieves the path that stores the content of DOI.
   */
  @Override
  public Optional<Path> get(DOI doi) {
    Path doiPath = getDoiPath(doi);
    return doiPath.toFile().exists() ? Optional.of(doiPath) : Optional.empty();
  }

  /**
   * Gets the file content, if exists, of file stored in the directory assigned to a DOI.
   */
  @Override
  public Optional<InputStream> getFile(DOI doi, String fileName) {
    return get(doi).map(doiPath -> doiPath.resolve(fileName))
                                    .map(Path::toFile)
                                      .map(file -> {
                                                      try {
                                                        return new FileInputStream(file);
                                                      } catch (FileNotFoundException ex) {
                                                        LOG.warn("Requested file {} not found",fileName, ex);
                                                        return null;
                                                      }
                                      });
  }

  /**
   * Resolves a path for a DOI.
   */
  private Path getDoiPath(DOI doi) {
    return storePath.resolve(doi.getPrefix() + '-' + doi.getSuffix());
  }
}
