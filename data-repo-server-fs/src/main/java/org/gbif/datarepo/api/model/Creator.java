package org.gbif.datarepo.api.model;


import org.gbif.datarepo.api.validation.constraints.ValidIdentifierScheme;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataPackage associated author.
 */
@ValidIdentifierScheme
public class Creator {

  public enum IdentifierScheme {

    ORCID("https://orcid.org"), ISNI("http://www.isni.org"), OTHER("");

    private String schemeURI;

    IdentifierScheme(String schemeURI){
      this.schemeURI = schemeURI;
    }

    public String getSchemeURI() {
      return schemeURI;
    }

  }

  @JsonIgnore
  private Integer key;

  @JsonIgnore
  private UUID dataPackageKey;

  @NotNull
  @JsonProperty
  private String name;

  @JsonProperty
  private List<String> affiliation;

  @JsonProperty
  private String identifier;

  @JsonProperty
  private IdentifierScheme identifierScheme;

  @JsonProperty
  private String schemeURI;

  @JsonIgnore
  @JsonFormat(shape=JsonFormat.Shape.STRING, pattern=DataPackage.ISO_DATE_FORMAT)
  private Date created;

  @JsonIgnore
  private String createdBy;

  /**
   * Author's data base key.
   */
  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  /**
   * Associated data package identifier.
   */
  public UUID getDataPackageKey() {
    return dataPackageKey;
  }

  public void setDataPackageKey(UUID dataPackageKey) {
    this.dataPackageKey = dataPackageKey;
  }

  /**
   * Author's first name and given names.
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Author's affiliation to DataPackage.
   */
  public List<String> getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(List<String> affiliation) {
    this.affiliation = affiliation;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public IdentifierScheme getIdentifierScheme() {
    return identifierScheme;
  }

  public void setIdentifierScheme(IdentifierScheme identifierScheme) {
    this.identifierScheme = identifierScheme;
  }

  public String getSchemeURI() {
    return schemeURI;
  }

  public void setSchemeURI(String schemeURI) {
    this.schemeURI = schemeURI;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}
