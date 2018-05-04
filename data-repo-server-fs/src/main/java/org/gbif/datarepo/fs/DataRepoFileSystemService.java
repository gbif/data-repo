package org.gbif.datarepo.fs;

import org.apache.commons.codec.digest.DigestUtils;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.impl.download.FileDownload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRepoFileSystemService {

  private static final Logger LOG = LoggerFactory.getLogger(DataRepoFileSystemService.class);

  /**
   * Paths where the files are stored.
   */
  private final Path storePath;

  /**
   * FileSystem implementation.
   */
  private final FileSystem fileSystem;


  private final FileDownload fileDownload;

  @Inject
  public DataRepoFileSystemService(Path storePath, FileSystem fileSystem) {
    try {
      this.storePath = storePath;
      this.fileSystem = fileSystem;
      fileDownload = new FileDownload(fileSystem);
      //Create directory if it doesn't exist
      if (!fileSystem.exists(storePath)) {
        Preconditions.checkState(fileSystem.mkdirs(storePath), "Error creating data directory");
      }
      Preconditions.checkArgument(fileSystem.isDirectory(storePath), "Repository is not a directory");
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Stores an input stream as the specified file name under the directory assigned to the DOI parameter.
   * Returns the new path where the file is stored.
   */
  public Path store(UUID dataPackageKey, FileInputContent fileInputContent) {
    try {
      Path dpPath = getPath(dataPackageKey);
      if (!fileSystem.exists(dpPath)) {
        fileSystem.mkdirs(dpPath);
      }
      Path newFilePath = resolve(dpPath, fileInputContent.getName());
      fileDownload.copy(fileInputContent, newFilePath, fileSystem);
      return newFilePath;
    } catch (IOException ex) {
      LOG.error("Error storing file {}", fileInputContent.getName(), ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Opens an InputStream to the content of a data package file.
   */
  public InputStream openDataPackageFile(UUID dataPackageKey, String fileName) throws IOException {
    return fileSystem.open(resolve(getPath(dataPackageKey), fileName));
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
  private static Path resolve(Path path, String extPath) {
    return new Path(path.toString() + '/' + extPath);
  }

  /**
   * Removes all files and directories of a data package directory.
   */
  public void clearDataPackageDir(UUID dataPackageKey) {
    try {
      Path dir = getPath(dataPackageKey);
      deleteDataPackage(dataPackageKey);
      fileSystem.mkdirs(dir);
    } catch (IOException ex) {
      LOG.error("Error deleting data package content {}", dataPackageKey, ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Recursively removes a data package directory.
   */
  public void deleteDataPackage(UUID dataPackageKey) {
    try {
      Path dir = getPath(dataPackageKey);
      if (fileSystem.exists(dir)) {
        fileSystem.delete(dir, true);
      }
    } catch (IOException ex) {
      LOG.error("Error deleting data package directory {}", dataPackageKey, ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Calculates the MD5 hash of local File.
   */
  public static String md5(File localFile) {
    try {
      return Files.hash(localFile, Hashing.md5()).toString();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Calculates the MD5 hash of local File content.
   */
  public String md5(Path file) {
    try {
      if (fileSystem instanceof RawLocalFileSystem) {
        return md5(new File(file.toUri().getPath()));
      }
      return md5Hdfs(file);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
    *  Reads a HDFS file and calculates its MD5 hash.
    */
  private String md5Hdfs(Path file) throws IOException {
   try(InputStream inputStream = new BufferedInputStream(fileSystem.open(file))) {
     return DigestUtils.md5Hex(inputStream);
   }
  }


  /**
   * Calculates the MD5 hash of File content.
   */
  public long fileSize(Path file) {
    try {
      return fileSystem.getFileStatus(file).getLen();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
