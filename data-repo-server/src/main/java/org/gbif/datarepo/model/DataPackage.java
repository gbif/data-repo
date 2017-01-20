package org.gbif.datarepo.model;

import org.gbif.api.model.common.DOI;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize
public class DataPackage {

  @JsonProperty
  private DOI doi;

  @JsonProperty
  private String targetUrl;

  @JsonProperty
  private String metadataFile = "metadata.xml";

  @JsonProperty
  private List<String> files;

  public DataPackage() {
    files = new ArrayList<>();
  }
  public DOI getDoi() {
    return doi;
  }

  public void setDoi(DOI doi) {
    this.doi = doi;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  public String getMetadataFile() {
    return metadataFile;
  }

  public void setMetadataFile(String metadataFile) {
    this.metadataFile = metadataFile;
  }

  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  public void addFile(String fileName){
    files.add(fileName);
  }
}
