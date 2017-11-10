package org.gbif.datarepo.api.model;


import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 */
@JsonSerialize
public class AlternativeIdentifier {

  /**
   * Types of supported identifiers.
   */
  public enum Type {
    URL, LSID, DOI, UUID, URI, UNKNOWN;
  }

  @JsonIgnore
  private UUID dataPackageKey;

  @JsonProperty
  private String identifier;

  @JsonProperty
  private Type type;

  @JsonProperty
  private String createdBy;

  @JsonProperty
  private Date created;

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


  @Override
  public int hashCode() {
    return Objects.hash(identifier, type, dataPackageKey, createdBy, created);
  }

  @Override
  public String toString() {
    return "{\"identifier\": \"" + identifier
           + "\", \"type\": \"" + type
           + "\", \"dataPackageKey\": \"" + dataPackageKey
           + "\", \"createdBy\": \"" + createdBy
           + "\", \"created\": \"" + created + "\"}";
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AlternativeIdentifier other = (AlternativeIdentifier) obj;
    return Objects.equals(identifier, other.identifier)
           && Objects.equals(type, other.type)
           && Objects.equals(dataPackageKey, other.dataPackageKey)
           && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(created, other.created);

  }
}
