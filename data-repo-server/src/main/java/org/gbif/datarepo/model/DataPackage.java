package org.gbif.datarepo.model;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class represents a data package, which contains: a metadata file, a DOI and a list of containing files.
 */
@JsonSerialize
public class DataPackage {

  @JsonProperty
  private URI doi;

  @JsonProperty
  private String metadata;

  @JsonProperty
  private Set<String> files;

  private final String baseUrl;

  /**
   * Default constructor.
   * Set the base url to empty and initialises the list of files.
   */
  public DataPackage() {
    files = new HashSet<>();
    baseUrl = "";
  }


  /**
   * Base Url constructor.
   * Set the base url to specified value and initialises the list of files.
   */
  public DataPackage(String baseUrl) {
    files = new HashSet<>();
    this.baseUrl = baseUrl;
  }

  /**
   * Data package assigned DOI
   */
  public URI getDoi() {
    return doi;
  }

  public void setDoi(URI doi) {
    this.doi = doi;
  }

  /**
   * Metadata associated to the data package.
   */
  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = baseUrl + metadata;
  }

  /**
   * List of containing files (excluding the metadata.xml file).
   */
  public Set<String> getFiles() {
    return files;
  }

  public void setFiles(Set<String> files) {
    this.files = files;
  }

  /**
   * Adds a new file to the list of containing files.
   * The baseUrl is prepend to the file name.
   */
  public void addFile(String fileName) {
    files.add(baseUrl + fileName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final DataPackage other = (DataPackage) obj;
    return Objects.equals(doi, other.doi)
           && Objects.equals(metadata, other.metadata)
           && Objects.equals(files, other.files);

  }

  @Override
  public int hashCode() {
    return Objects.hash(doi, metadata, files);
  }

  @Override
  public String toString() {
    return "{\"doi\": \"" + Objects.toString(doi) + "\", \"metadata\": \"" + Objects.toString(metadata) +
           "\", \"files\": \"" + Objects.toString(files) + "\"}";
  }
}
