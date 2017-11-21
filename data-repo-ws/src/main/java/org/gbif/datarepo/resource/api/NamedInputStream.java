package org.gbif.datarepo.resource.api;

import java.io.InputStream;

public class NamedInputStream {

  private final String name;

  private final InputStream inputStream;

  public NamedInputStream(String name, InputStream inputStream) {
    this.name = name;
    this.inputStream = inputStream;
  }

  public String getName() {
    return name;
  }

  public InputStream getInputStream() {
    return inputStream;
  }
}
