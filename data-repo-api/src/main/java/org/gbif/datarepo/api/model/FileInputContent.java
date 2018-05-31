package org.gbif.datarepo.api.model;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * Represents an InputStream and its associated name.
 * It's used for the convenience from sending file streams with its associated names.
 */
public class FileInputContent {

  private final String name;

  private final InputStream inputStream;

  private final URI fileLocation;

  /**
   * Full constructor, creates an instance using a name and inputStream objects.
   */
  public FileInputContent(String name, InputStream inputStream, URI fileLocation) {
    this.name = name;
    this.inputStream = inputStream;
    this.fileLocation = fileLocation;
  }

  /**
   * Factory method, creates an instance using the name and inputStream objects.
   */
  public static FileInputContent from(String name, InputStream inputStream) {
    return new FileInputContent(name, inputStream, null);
  }

  /**
   * Factory method, creates an instance using the name and inputStream objects.
   */
  public static FileInputContent from(String name, URI fileLocation) {
    return new FileInputContent(name, null, fileLocation);
  }

  /**
   * Name from the InputStream, normally it's the file name.
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

  /**
   * External location of the submitted file. This field can be null.
   */
  public URI getFileLocation() {
    return fileLocation;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FileInputContent other = (FileInputContent) obj;
    return Objects.equals(name, other.name)
           && Objects.equals(inputStream, other.inputStream)
           && Objects.equals(fileLocation, other.fileLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, inputStream, fileLocation);
  }

  @Override
  public String toString() {
    return "{\"name\": \"" + name
           + "\", \"inpuStream\": \"" + Objects.toString(inputStream)
           + "\", \"fileLocation\": \"" + Objects.toString(fileLocation)
           + "\"}";
  }

}
