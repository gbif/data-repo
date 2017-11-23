package org.gbif.datarepo.resource.api;

import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.model.Identifier;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = DataPackageRequest.Builder.class)
@JsonSerialize
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
  @Nullable
  public abstract Set<String> getTags();

  @NotNull
  @Valid
  @JsonProperty(required = true)
  public abstract Set<Creator> getCreators();

  @JsonProperty
  @Nullable
  public abstract Set<Identifier> getRelatedIdentifiers();

  @JsonProperty
  @Nullable
  public abstract List<String> getContentFiles();

  @JsonIgnore
  @Nullable
  public abstract List<NamedInputStream> getContentInputStreams();

  @JsonIgnore
  @Nullable
  public abstract NamedInputStream getIdentifiersInputStream();

  @JsonProperty
  @Nullable
  public abstract String getRelatedIdentifierFile();

  @JsonCreator
  public static Builder builder() {
    return new AutoValue_DataPackageRequest.Builder();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "")
  public abstract static class Builder {

    @JsonCreator private static Builder create() { return builder(); }

    @JsonProperty("title")
    public abstract Builder setTitle(String newTitle);

    @JsonProperty("description")
    public abstract Builder setDescription(String newDescription);

    @JsonProperty("license")
    public abstract Builder setLicense(License newLicense);

    @JsonProperty("tags")
    @Nullable
    public abstract Builder setTags(Set<String> newTags);

    @JsonProperty("creators")
    public abstract Builder setCreators(Set<Creator> newCreators);

    @JsonProperty("relatedIdentifiers")
    @Nullable
    public abstract Builder setRelatedIdentifiers(Set<Identifier> newRelatedIdentifiers);

    @JsonProperty("contentFiles")
    @Nullable
    public abstract Builder setContentFiles(List<String> newContentFiles);

    @JsonIgnore
    @Nullable
    public abstract Builder setContentInputStreams(List<NamedInputStream> newContentInputStreams);

    @JsonIgnore
    @Nullable
    public abstract Builder setIdentifiersInputStream(NamedInputStream newIdentifiersInputStream);

    @JsonProperty("relatedIdentifierFile")
    @Nullable
    public abstract Builder setRelatedIdentifierFile(String newRelatedIdentifierFile);

    public abstract DataPackageRequest build();
  }
}
