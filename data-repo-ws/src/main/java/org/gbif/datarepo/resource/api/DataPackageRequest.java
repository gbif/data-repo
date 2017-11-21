package org.gbif.datarepo.resource.api;

import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.model.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = DataPackageRequest.Builder.class)
public abstract class DataPackageRequest {

  @NotNull
  @JsonProperty(required = true)
  public abstract String getTitle();

  @NotNull
  @JsonProperty(required = true)
  public abstract String getDescription();

  @NotNull
  @JsonProperty(required = true)
  public abstract License getLicense();

  @JsonProperty
  public abstract Set<String> getTags();

  @NotNull
  @Valid
  @JsonProperty(required = true)
  public abstract Set<Creator> getCreators();

  @JsonProperty
  public abstract Set<Identifier> getRelatedIdentifiers();

  @JsonProperty
  public abstract List<String> getContentFiles();

  @JsonIgnore
  public abstract List<NamedInputStream> getContentInputStreams();

  @JsonIgnore
  public abstract NamedInputStream getIdentifiersInputStream();

  @JsonProperty
  public abstract String getRelatedIdentifierFile();

  @JsonCreator
  public static DataPackageRequest create(
    @NotNull @JsonProperty("title") String newTitle,
    @NotNull @JsonProperty("description") String newDescription,
    @NotNull @JsonProperty("license") License newLicense,
    @Nullable @JsonProperty("tags") Set<String> newTags,
    @NotNull @JsonProperty("creators") Set<Creator> newCreators,
    @Nullable @JsonProperty("relatedIdentifiers") Set<Identifier> newRelatedIdentifiers,
    @Nullable @JsonProperty("contentFiles") List<String> newContentFiles,
    @Nullable @JsonProperty("relatedIdentifierFile") String newRelatedIdentifierFile
  ) {
    return builder().setTitle(newTitle)
      .setDescription(newDescription)
      .setLicense(newLicense)
      .setTags(newTags)
      .setCreators(newCreators)
      .setRelatedIdentifiers(newRelatedIdentifiers)
      .setContentFiles(newContentFiles)
      .setRelatedIdentifierFile(newRelatedIdentifierFile)
      .build();
  }

  public static Builder builder() {
    return new AutoValue_DataPackageRequest.Builder()
      .setContentFiles(new ArrayList<>())
      .setContentInputStreams(new ArrayList<>())
      .setRelatedIdentifiers(new HashSet<>())
      .setRelatedIdentifierFile("");
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "")
  public abstract static class Builder {

    @JsonCreator private static Builder create() { return DataPackageRequest.builder(); }

    @NotNull
    @JsonProperty("title")
    public abstract Builder setTitle(String newTitle);

    @NotNull
    @JsonProperty("description")
    public abstract Builder setDescription(String newDescription);

    @NotNull
    @JsonProperty("license")
    public abstract Builder setLicense(License newLicense);

    @JsonProperty("tags")
    public abstract Builder setTags(Set<String> newTags);

    @NotNull
    @JsonProperty("creators")
    public abstract Builder setCreators(Set<Creator> newCreators);

    @JsonProperty("relatedIdentifiers")
    public abstract Builder setRelatedIdentifiers(Set<Identifier> newRelatedIdentifiers);

    @JsonProperty("contentFiles")
    public abstract Builder setContentFiles(List<String> newContentFiles);

    @JsonIgnore
    public abstract Builder setContentInputStreams(List<NamedInputStream> newContentInputStreams);

    @JsonIgnore
    public abstract Builder setIdentifiersInputStream(NamedInputStream newIdentifiersInputStream);

    @JsonProperty("relatedIdentifierFile")
    public abstract Builder setRelatedIdentifierFile(String newRelatedIdentifierFile);

    public abstract DataPackageRequest build();
  }
}
