package org.gbif.datarepo.api.model;

import org.gbif.api.model.common.DOI;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag {

  @JsonProperty
  private Integer key;

  @JsonIgnore
  private DOI dataPackageDoi;

  @JsonProperty
  private String value;

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

  public DOI getDataPackageDoi() {
    return dataPackageDoi;
  }

  public void setDataPackageDoi(DOI dataPackageDoi) {
    this.dataPackageDoi = dataPackageDoi;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
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
    return Objects.hash(key, value, dataPackageDoi, createdBy, created);
  }

  @Override
  public String toString() {
    return "{\"key\": \"" + key
           + "\", \"value\": \"" + value
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
    Tag other = (Tag) obj;
    return Objects.equals(key, other.key)
           && Objects.equals(value, other.value)
           && Objects.equals(dataPackageDoi, other.dataPackageDoi)
           && Objects.equals(createdBy, other.createdBy)
           && Objects.equals(created, other.created);

  }
}
