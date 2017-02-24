package org.gbif.datarepo.api.model;

import java.io.InputStream;
import java.util.Objects;

/**
 * Represents an InputStream and its associated name.
 * It's used for the convenience from sending file streams with its associated names.
 */
public class FileInputContent {

  private final String name;

  private final InputStream inputStream;

  /**
   * Full constructor, creates an instance using a name and inputStream objects.
   */
  public FileInputContent(String name, InputStream inputStream) {
    this.name = name;
    this.inputStream = inputStream;
  }

  /**
   * Factory method, creates an instance using the name and inputStream objects.
   */
  public static FileInputContent from(String name, InputStream inputStream) {
    return new FileInputContent(name, inputStream);
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


  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    FileInputContent other = (FileInputContent) obj;
    return Objects.equals(name, other.name)
           && Objects.equals(inputStream, other.inputStream);

  }

  @Override
  public int hashCode() {
    return Objects.hash(name, inputStream);
  }

  @Override
  public String toString() {
    return "{\"name\": \"" + name + "\", \"inpuStream\": \"" + Objects.toString(inputStream) + "\"}";
  }

}
