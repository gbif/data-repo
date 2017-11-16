package org.gbif.datarepo.api.model;

import org.gbif.api.model.common.DOI;
import org.gbif.api.vocabulary.License;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(value = "relatedIdentifiers", allowSetters = true)
public class DataPackage {

  public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  @JsonProperty
  private UUID key;

  @NotNull
  @JsonProperty
  private String title;

  @NotNull
  @JsonProperty
  private String description;

  @JsonProperty
  @JsonSerialize(using = DOISerializer.class)
  @JsonDeserialize(using = DOIDeserializer.class)
  private DOI doi;

  @NotNull
  @JsonProperty
  private Set<DataPackageFile> files;

  @JsonProperty
  @JsonFormat(shape=JsonFormat.Shape.STRING, pattern=ISO_DATE_FORMAT)
  private Date created;

  @JsonProperty
  @JsonFormat(shape=JsonFormat.Shape.STRING, pattern=ISO_DATE_FORMAT)
  private Date modified;

  @JsonIgnore
  @JsonFormat(shape=JsonFormat.Shape.STRING, pattern=ISO_DATE_FORMAT)
  private Date deleted;

  @JsonProperty
  private String createdBy;

  @JsonProperty
  private String checksum;

  @JsonProperty
  private long size;

  @JsonProperty
  @Valid
  private Set<Identifier> relatedIdentifiers;

  @JsonProperty
  private Set<Tag> tags;

  @NotNull
  @Valid
  @JsonProperty
  private Set<Creator> creators;

  @NotNull
  @JsonProperty
  private License license;

  @JsonProperty
  private String citation;

  private final String baseUrl;

  /**
   * Default constructor.
   * Set the base url to empty and initialises the list from files.
   */
  public DataPackage() {
    files = new HashSet<>();
    relatedIdentifiers = new HashSet<>();
    tags = new HashSet<>();
    creators = new HashSet<>();
    baseUrl = "";
  }


  /**
   * Base Url constructor.
   * Set the base url to specified value and initialises the list from files.
   */
  public DataPackage(String baseUrl) {
    files = new HashSet<>();
    relatedIdentifiers = new HashSet<>();
    tags = new HashSet<>();
    creators = new HashSet<>();
    this.baseUrl = baseUrl;
  }

  /**
   * Data base key.
   */
  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
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
   * List from containing files (excluding the metadata.xml file).
   */
  public Set<DataPackageFile> getFiles() {
    return files;
  }

  public void setFiles(Set<DataPackageFile> files) {
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
   *
   * @return estimated size in bytes
   */
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  /**
   * This checksum is calculated by combining all checksums of contained files.
   * @return derived data package checksum
   */
  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  /**
   * External and alternative identifier that uniquely identify this data package.
   */
  public Set<Identifier> getRelatedIdentifiers() {
    return relatedIdentifiers;
  }

  public void setRelatedIdentifiers(Set<Identifier> relatedIdentifiers) {
    this.relatedIdentifiers = relatedIdentifiers;
  }

  /**
   * Tags associated to the DataPackage.
   */
  public Set<Tag> getTags() {
    return tags;
  }

  public void setTags(Set<Tag> tags) {
    this.tags = tags;
  }

  /**
   * DataPackage authors.
   */
  public Set<Creator> getCreators() {
    return creators;
  }

  public void setCreators(Set<Creator> creators) {
    this.creators = creators;
  }

  /**
   * Licensed applied to the DataPackage.
   */
  public License getLicense() {
    return license;
  }

  /**
   * <pre>
   * {@code
   * <intellectualRights>
   *   <para>This work is licensed under a <ulink url="http://creativecommons.org/licenses/by/4.0/legalcode"><citetitle>Creative Commons Attribution (CC-BY) 4.0 License</citetitle></ulink>.</para>
   * </intellectualRights>
   * }
   * </pre>
   */
  public void setLicense(License license) {
    this.license = license;
  }

  /**
   * The exact form of how to cite this DataPackage.
   */
  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }

  /**
   * Adds a new file to the list from containing files.
   * The baseUrl is prepend to the file name.
   */
  public void addFile(String fileName, String checksum, long size) {
    files.add(new DataPackageFile(baseUrl + fileName, checksum, size));
  }

  /**
   * Adds a new file to the list of containing files.
   * The baseUrl is prepend to the file name.
   */
  public void addFile(DataPackageFile file) {
    files.add(new DataPackageFile(baseUrl + file.getFileName(), file.getChecksum(), file.getSize()));
  }

  /**
   * Adds a new Creator to the list.
   */
  public void addCreator(Creator creator) {
    creator.setDataPackageKey(key);
    creator.setCreatedBy(createdBy);
    creators.add(creator);
  }


  /**
   * Adds a new AlternativeIdentifier to the list of relatedIdentifiers.
   */
  public void addRelatedIdentifier(Identifier relatedIdentifier) {
    relatedIdentifier.setDataPackageKey(key);
    relatedIdentifier.setCreatedBy(createdBy);
    relatedIdentifiers.add(relatedIdentifier);
  }

  /**
   * Adds a new Tag to the list of tags.
   */
  public void addTag(Tag tag) {
    tag.setDataPackageKey(key);
    tag.setCreatedBy(createdBy);
    tags.add(tag);
  }

  /**
   * Adds a new Tag to the list of tags.
   */
  public void addTag(String tagValue) {
    Tag tag = new Tag();
    tag.setValue(tagValue);
    tag.setDataPackageKey(key);
    tag.setCreatedBy(createdBy);
    tags.add(tag);
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
           && Objects.equals(files, other.files)
           && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(title, other.title)
           && Objects.equals(description, other.description)
           && Objects.equals(created, other.created)
           && Objects.equals(modified, other.modified)
           && Objects.equals(size, other.size)
           && Objects.equals(checksum, other.checksum)
           && Objects.equals(relatedIdentifiers, other.relatedIdentifiers)
           && Objects.equals(tags, other.tags)
           && Objects.equals(creators, other.creators)
           && Objects.equals(citation, other.citation)
           && Objects.equals(license, other.license);

  }

  @Override
  public int hashCode() {
    return Objects.hash(doi, files, createdBy, created, title, description, modified, size, checksum,
                        relatedIdentifiers, tags, creators, citation, license);
  }

  @Override
  public String toString() {
    return "{\"doi\": \"" + Objects.toString(doi)
           + "\", \"files\": \"" + Objects.toString(files)
           + "\", \"createdBy\": \"" + createdBy
           + "\", \"title\": \"" + title
           + "\", \"description\": \"" + description
           + "\", \"created\": \"" + created
           + "\", \"modified\": \"" + modified
           + "\", \"checksum\": \"" + checksum
           + "\", \"size\": \"" + size
           + "\", \"relatedIdentifiers\": \"" + relatedIdentifiers
           + "\", \"creators\": \"" + creators
           + "\", \"citation\": \"" + citation
           + "\", \"license\": \"" + license
           + "\", \"tags\": \"" + tags + "\"}";
  }

  /**
   * Creates a clone from the current object but all the files are rebased to the specified URL.
   */
  public DataPackage inUrl(String url) {
    DataPackage dataPackage = new DataPackage(url);
    dataPackage.setDoi(doi);
    dataPackage.setKey(key);
    files.stream().forEach(dataPackage::addFile);
    dataPackage.setCreatedBy(createdBy);
    dataPackage.setCreated(created);
    dataPackage.setDeleted(deleted);
    dataPackage.setModified(modified);
    dataPackage.setTitle(title);
    dataPackage.setDescription(description);
    dataPackage.setChecksum(checksum);
    dataPackage.setSize(size);
    dataPackage.setRelatedIdentifiers(relatedIdentifiers);
    dataPackage.setTags(tags);
    dataPackage.setCreators(creators);
    dataPackage.setCitation(citation);
    dataPackage.setLicense(license);
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
