package org.gbif.datarepo.api.model;

import org.gbif.api.vocabulary.IdentifierType;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataPackage identifier.
 */
public class Identifier {

  @JsonProperty
  private String identifier;

  @JsonProperty
  private IdentifierType type;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public IdentifierType getType() {
    return type;
  }

  public void setType(IdentifierType type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Identifier that = (Identifier) o;
    return Objects.equals(identifier, that.identifier) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, type);
  }

  @Override
  public String toString() {
    return "Identifier{" +
           "identifier='" + identifier + '\'' +
           ", type=" + type +
           '}';
  }
}
