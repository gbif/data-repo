package org.gbif.datarepo.snapshots.hive;

import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.inject.DataRepoFsModule;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.occurrence.download.hive.DownloadTerms;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.datarepo.snapshots.hive.DownloadsConfig.DownloadInfo;
import static org.gbif.datarepo.snapshots.hive.DownloadsConfig.Format;
import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateEmlMetadata;
import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateRdf;

/**
 * Utility class to export GBIF snapshots into DataOne.
 */
class DownloadsExport {

  private final DownloadsConfig config;

  private final DataPackageManager dataPackageManager;

  private final FileSystem fileSystem;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Logger LOG = LoggerFactory.getLogger(DownloadsExport.class);

  static {
    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private DownloadsExport(DownloadsConfig config) {
    this.config = config;
    DataRepoFsModule dataRepoFsModule = new DataRepoFsModule(config.getDataRepoConfiguration(), null, null);
    dataPackageManager = new DataPackageManager(dataRepoFsModule.dataRepository(OBJECT_MAPPER));
    fileSystem = initFileSystem();
  }

  /**
   * Initializes a Hadoop FileSystem using the config files reachable in the classpath.
   */
  private static FileSystem initFileSystem() {
    try {
      return FileSystem.newInstance(new org.apache.hadoop.conf.Configuration());
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Creates the EML metadata for the snapshot table and persists it into the DataRepo.
   */
  private DataPackage createEmlMetadata(Collection<Term> terms, Path exportFile, String doi,
                                        DownloadInfo downloadInfo) {
    try {
      File file = generateEmlMetadata(terms,
                                      downloadInfo.date,
                                      exportFile.getName(),
                                      fileSystem.getStatus(exportFile).getCapacity(),
                                      downloadInfo.totalRecords,
                                      doi);
      DataPackage dataPackageCreated = dataPackageManager.createSnapshotEmlDataPackage(file, doi);
      //The file can be deleted since it was copied into the DataRepo
      file.delete();
      return dataPackageCreated;
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  /**
   * Creates the RDF metadata for the snapshot table and persists it into the DataRepo.
   */

  private void createRdf(String doi, UUID dataObjectId, UUID emlId, DownloadInfo downloadInfo) {
    try {
      UUID rdfId = UUID.randomUUID();
      File file = generateRdf(downloadInfo.date, dataObjectId, emlId, rdfId);
      dataPackageManager.createSnapshotRdfDataPackage(file, doi, rdfId);
      //The file can be deleted since it was copied into the DataRepo
      file.delete();
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  /**
   * Export process: creates a zip file of the snapshot table, an EML metadata file and a RDF description.
   */
  private void run() {
    // TODO: get latest download, DOI and number of records from registry WS. For now we set it manually in the config

    for (DownloadInfo downloadInfo : config.getDownloads()) {
      Path exportPath = new Path(downloadInfo.path);
      LOG.info("Creating a data package for the CSV download");
      DataPackage dataPackageData =
        dataPackageManager.createSnapshotDataPackageFromDownload(fileSystem.getUri().resolve(exportPath.toUri()),
                                                                 downloadInfo.doi);
      LOG.info("Deleting temporary files");
      LOG.info("Creating EML metadata");
      DataPackage dataPackageEml = createEmlMetadata(getDownloadFields(downloadInfo.format),
                                                     exportPath,
                                                     dataPackageData.getDoi().toString(),
                                                     downloadInfo);
      LOG.info("Creating RDF metadata");
      createRdf(dataPackageData.getDoi().toString(), dataPackageData.getKey(), dataPackageEml.getKey(), downloadInfo);
    }
  }

  private Set<Term> getDownloadFields(Format format) {
    if (format == Format.DWCA) {
      return ImmutableSet.<Term>builder().addAll(DownloadTerms.DOWNLOAD_VERBATIM_TERMS)
        .addAll(DownloadTerms.DOWNLOAD_INTERPRETED_TERMS)
        .add(GbifTerm.gbifID)
        .build();
    } else {
      return ImmutableSet.<Term>builder().addAll(DownloadTerms.SIMPLE_DOWNLOAD_TERMS).add(GbifTerm.gbifID).build();
    }
  }

  /**
   * Runs the export process using a configuration YAML file as parameter.
   */
  public static void main(String[] arg) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    DownloadsConfig config = objectMapper.readValue(new File(arg[0]), DownloadsConfig.class);
    new DownloadsExport(config).run();
  }

}
