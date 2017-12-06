package org.gbif.datarepo.resource.api;

import java.io.InputStream;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NamedInputStream {

  public abstract String getName();

  public abstract InputStream getInputStream();

  public static Builder builder() {return new AutoValue_NamedInputStream.Builder();}

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setName(String newName);

    public abstract Builder setInputStream(InputStream newInputStream);

    public abstract NamedInputStream build();
  }
}
