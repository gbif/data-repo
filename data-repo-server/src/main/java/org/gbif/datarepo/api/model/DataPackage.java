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
 * This class represents a data package, which contains: a metadata file, a DOI and a list from containing files.
 */
@JsonSerialize
public class DataPackage {

  public static final String METADATA_FILE = "metadata.xml";

  @JsonProperty
  private String title;

  @JsonProperty
  private String description;

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
   * Set the base url to empty and initialises the list from files.
   */
  public DataPackage() {
    files = new ArrayList<>();
    baseUrl = "";
  }


  /**
   * Base Url constructor.
   * Set the base url to specified value and initialises the list from files.
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
   * List from containing files (excluding the metadata.xml file).
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
   * Date from last modification.
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
   * A name or title by which a resource is known.
   */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Additional information about this resource.
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Adds a new file to the list from containing files.
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
           && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(title, other.title)
           && Objects.equals(description, other.description)
           && Objects.equals(created, other.created)
           && Objects.equals(modified, other.modified);

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
            + "\", \"createdBy\": \"" + createdBy
            + "\", \"title\": \"" + title
            + "\", \"description\": \"" + description
            + "\", \"created\": \"" + created
            + "\", \"modified\": \"" + modified + "\"}";
  }

  /**
   * Creates a clone from the current object btu all the files are rebased to the specified URL.
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
    dataPackage.setTitle(title);
    dataPackage.setDescription(description);
    return dataPackage;
  }

  /**
   * Creates a clone from the current object btu all the files are rebased to the specified URL.
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
