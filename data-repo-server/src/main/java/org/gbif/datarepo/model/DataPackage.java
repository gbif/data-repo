package org.gbif.datarepo.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
  private String metadata = "metadata.xml";

  @JsonProperty
  private List<String> files;

  private final String baseUrl;

  /**
   * Default constructor.
   * Set the base url to empty and initialises the list of files.
   */
  public DataPackage() {
    files = new ArrayList<>();
    baseUrl = "";
  }


  /**
   * Base Url constructor.
   * Set the base url to specified value and initialises the list of files.
   */
  public DataPackage(String baseUrl) {
    files = new ArrayList<>();
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
    return  baseUrl + metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  /**
   * List of containing files (excluding the metadata.xml file).
   */
  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  /**
   * Adds a new file to the list of containing files.
   * The baseUrl is prepend to the file name.
   */
  public void addFile(String fileName) {
    files.add(baseUrl + fileName);
  }

}
