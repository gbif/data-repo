package org.gbif.datarepo.api.model;


import java.util.Objects;

/**
 * File contained in a DataPackage and its checksum.
 */
public class DataPackageFile {

  private String fileName;

  private String format;

  private String checksum;

  private long size;

  /**
   * Default constructor, use for serialization.
   */
  public DataPackageFile() {
    //NOP
  }

  /**
   * Full constructor.
   */
  public DataPackageFile(String fileName, String format, String checksum, long size) {
    this.fileName = fileName;
    this.format = format;
    this.checksum = checksum;
    this.size = size;
  }

  /**
   * File name.
   */
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * File format.
   */
  public String getFormat() {
     return format;
  }

  public void setFormat(String format) {
     this.format = format;
  }

  /**
   * MD5 checksum.
   */
  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  /**
   * File fileSize in bytes.
   */
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, format, checksum, size);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DataPackageFile other = (DataPackageFile) obj;
    return Objects.equals(checksum, other.checksum)
           && Objects.equals(fileName, other.fileName)
           && Objects.equals(format, other.format)
           && Objects.equals(size, other.size);
  }

  @Override
  public String toString() {
    return "{\"fileName\": \"" + fileName
           + "\", \"format\": \"" + format
           + "\", \"checksum\": \"" + checksum
           + "\", \"fileSize\": \"" + size + "\"}";
  }
}
