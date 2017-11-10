package org.gbif.datarepo.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataPackage associated author.
 */
public class Creator {

  public enum IdentifierScheme {
    ORCID, ISNI, FUND_REF, OTHER;
  }

  @JsonIgnore
  private Integer key;

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
}
