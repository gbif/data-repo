package org.gbif.datarepo.api.model;

import org.gbif.api.model.common.DOI;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This class represents a data package, which contains: a metadata file, a DOI and a list of containing files.
 */
@JsonSerialize
public class DataPackage {

  public static final String METADATA_FILE = "metadata.xml";

  @JsonProperty
  @JsonSerialize(using = DOISerializer.class)
  @JsonDeserialize(using = DOIDeserializer.class)
  private DOI doi;

  @JsonProperty
  private String metadata;

  @JsonProperty
  private List<String> files;

  @JsonProperty
  private Date created;

  @JsonProperty
  private Date modified;

  @JsonProperty
  private Date deleted;

  @JsonIgnore
  private String createdBy;

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
   * Data package assigned DOI.
   */
  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
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
  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  /**
   * Creation date.
   */
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  /**
   * Date of last modification.
   */
  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  /**
   * Date when this data package was deleted.
   */
  public Date getDeleted() {
    return deleted;
  }

  public void setDeleted(Date deleted) {
    this.deleted = deleted;
  }

  /**
   * User that created this data package.
   */
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
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
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DataPackage other = (DataPackage) obj;
    return Objects.equals(doi, other.doi)
           && Objects.equals(metadata, other.metadata)
           && Objects.equals(files, other.files)
           && Objects.equals(createdBy, other.createdBy);

  }

  @Override
  public int hashCode() {
    return Objects.hash(doi, metadata, files);
  }

  @Override
  public String toString() {
    return "{\"doi\": \"" + Objects.toString(doi)
            + "\", \"metadata\": \"" + metadata
            + "\", \"files\": \"" + Objects.toString(files)
            + "\", \"createdBy\": \"" + createdBy + "\"}";
  }

  /**
   * Creates a clone of the current object btu all the files are rebased to the specified URL.
   */
  public DataPackage inUrl(String url) {
    DataPackage dataPackage = new DataPackage(url);
    dataPackage.setDoi(doi);
    files.stream().forEach(dataPackage::addFile);
    dataPackage.setMetadata(metadata);
    dataPackage.setCreatedBy(createdBy);
    dataPackage.setCreated(created);
    dataPackage.setDeleted(deleted);
    dataPackage.setModified(modified);
    return dataPackage;
  }

  /**
   * Creates a clone of the current object btu all the files are rebased to the specified URL.
   */
  public DataPackage inUrl(URI baseUrl) {
    return inUrl(baseUrl.toString());
  }


  /**
   * Serializes a DOI as doi name with a doi: scheme.
   * For example doi:10.1038/nature.2014.16460
   */
  public static class DOISerializer extends JsonSerializer<DOI> {

    @Override
    public void serialize(DOI value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(value.toString());
    }
  }

  /**
   * Deserializes a DOI from various string based formats.
   * See DOI constructor for details.
   */
  public static class DOIDeserializer extends JsonDeserializer<DOI> {

    @Override
    public DOI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p != null && p.getTextLength() > 0) {
        return new DOI(p.getText());
      }
      return null;
    }
  }

}
