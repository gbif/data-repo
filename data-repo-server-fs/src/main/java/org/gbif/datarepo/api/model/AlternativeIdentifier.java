package org.gbif.datarepo.api.model;

import org.gbif.api.model.common.DOI;

import java.util.Date;
import java.util.Objects;

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
  private DOI dataPackageDoi;

  @JsonProperty
  private String identifier;

  @JsonProperty
  private Type type;

  @JsonProperty
  private String createdBy;

  @JsonProperty
  private Date created;

  public DOI getDataPackageDoi() {
    return dataPackageDoi;
  }

  public void setDataPackageDoi(DOI dataPackageDoi) {
    this.dataPackageDoi = dataPackageDoi;
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
    return Objects.hash(identifier, type, dataPackageDoi, createdBy, created);
  }

  @Override
  public String toString() {
    return "{\"identifier\": \"" + identifier
           + "\", \"type\": \"" + type
           + "\", \"dataPackageDoi\": \"" + dataPackageDoi
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
           && Objects.equals(dataPackageDoi, other.dataPackageDoi)
           && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(created, other.created);

  }
}
