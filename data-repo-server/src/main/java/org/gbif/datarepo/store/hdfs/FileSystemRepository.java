package org.gbif.datarepo.store.hdfs;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.utils.file.FileUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Stores archives in a files system repository.
 */
public class FileSystemRepository implements DataRepository {

  /**
   * Paths where the archives are stored.
   */
  private final Path storePath;

  public FileSystemRepository(String storePath) {
    this.storePath = Paths.get(storePath);
  }

  @Override
  public void store(DOI doi, InputStream file, String fileName) {
    try {
      Path doiPath = getDoiPath(doi);
      if (!doiPath.toFile().exists()) {
        Files.createDirectory(doiPath);
      }
      Files.copy(file, doiPath.resolve(fileName));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void storeMetadata(DOI doi, InputStream file) {
    store(doi, file, "metadata.xml");
  }

  @Override
  public void delete(DOI doi) {
    Optional<Path> doiPath = get(doi);
    if (doiPath.isPresent()) {
      FileUtils.deleteDirectoryRecursively(doiPath.get().toFile());
    }
  }

  @Override
  public Optional<Path> get(DOI doi) {
    Path doiPath = getDoiPath(doi);
    return doiPath.toFile().exists() ? Optional.of(doiPath) : Optional.empty();
  }

  @Override
  public Optional<InputStream> getFile(DOI doi, String fileName) {
    return get(doi).map(doiPath -> doiPath.resolve(fileName))
                                    .map(Path::toFile)
                                      .map(file -> {
                                                      try {
                                                        return new FileInputStream(file);
                                                      } catch (FileNotFoundException ex) {
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
