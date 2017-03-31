package org.gbif.datarepo.api.model;

import java.util.Objects;

/**
 * File contained in a DataPackage and its checksum.
 */
public class DataPackageFile {

  private String fileName;

  private String checksum;

  /**
   * Default constructor, use for serialization.
   */
  public DataPackageFile(){
    //NOP
  }

  /**
   * Full constructor.
   */
  public DataPackageFile(String fileName, String checksum) {
    this.fileName = fileName;
    this.checksum = checksum;
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
   * MD5 checksum.
   */
  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, checksum);
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
           && Objects.equals(fileName, other.fileName);
  }

  @Override
  public String toString() {
    return "{\"fileName\": \"" + fileName
           + "\", \"checksum\": \"" + checksum + "\"}";
  }
}
