package org.gbif.datarepo.api.model;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 */
@JsonSerialize
public class Identifier {

  /**
   * Types of supported identifiers.
   */
  public enum Type {
    URL, LSID, DOI, UUID, URI, UNKNOWN
  }

  public enum RelationType {
    IsAlternativeOf,
    IsCitedBy,
    Cites,
    IsSupplementTo,
    IsSupplementedBy,
    IsContinuedBy,
    Continues,
    HasMetadata,
    IsMetadataFor,
    IsNewVersionOf,
    IsPreviousVersionOf,
    IsPartOf,
    HasPart,
    IsReferencedBy,
    References,
    IsDocumentedBy,
    Documents,
    IsCompiledBy,
    Compiles,
    IsVariantFormOf,
    IsOriginalFormOf,
    IsIdenticalTo,
    IsReviewedBy,
    Reviews,
    IsDerivedFrom,
    IsSourceOf,
  }

  @JsonIgnore
  private UUID dataPackageKey;

  @JsonProperty
  private Integer key;

  @NotNull
  @JsonProperty
  private String identifier;

  @NotNull
  @JsonProperty
  private Type type;

  @NotNull
  @JsonProperty
  private RelationType relationType = RelationType.IsAlternativeOf;

  @JsonProperty
  private String createdBy;

  @JsonProperty
  private Date created;


  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public UUID getDataPackageKey() {
    return dataPackageKey;
  }

  public void setDataPackageKey(UUID dataPackageKey) {
    this.dataPackageKey = dataPackageKey;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public RelationType getRelationType() {
    return relationType;
  }

  public void setRelationType(RelationType relationType) {
    this.relationType = relationType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, identifier, type, dataPackageKey, createdBy, created, relationType);
  }

  @Override
  public String toString() {
    return "{\"key\": \""
           + key
           +"\"identifier\": \""
           + identifier
           + "\", \"type\": \""
           + type
           + "\", \"dataPackageKey\": \""
           + dataPackageKey
           + "\", \"createdBy\": \""
           + createdBy
           + "\", \"created\": \""
           + created
           + "\", \"relationType\": \""
           + relationType
           + "\"}";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Identifier other = (Identifier) obj;
    return Objects.equals(key, other.key) &&
           Objects.equals(identifier, other.identifier) && Objects.equals(type, other.type)
           && Objects.equals(dataPackageKey,other.dataPackageKey) && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(created, other.created) && Objects.equals(relationType, other.relationType);

  }
}
