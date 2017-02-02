package org.gbif.datarepo.api;

import java.io.InputStream;

/**
 * Represents an InputStream and its associated name.
 * It's used for the convenience of sending file streams with its associated names.
 */
public class FileInputContent {

  private final String name;

  private final InputStream inputStream;

  /**
   * Factory method, creates an instance using the name and inputStream objects.
   */
  public static FileInputContent of(String name, InputStream inputStream) {
    return new FileInputContent(name, inputStream);
  }

  /**
   * Full constructor, creates an instance using a name and inputStream objects.
   */
  public FileInputContent(String name, InputStream inputStream) {
    this.name = name;
    this.inputStream = inputStream;
  }

  /**
   * Name of the InputStream, normally it's the file name.
   */
  public String getName() {
    return name;
  }

  /**
   * Open InputStream.
   */
  public InputStream getInputStream() {
    return inputStream;
  }

}
