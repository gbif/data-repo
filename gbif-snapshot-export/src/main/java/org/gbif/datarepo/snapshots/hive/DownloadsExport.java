package org.gbif.datarepo.snapshots.hive;

import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.inject.DataRepoFsModule;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.hadoop.compress.d2.D2CombineInputStream;
import org.gbif.hadoop.compress.d2.D2Utils;
import org.gbif.hadoop.compress.d2.zip.ModalZipOutputStream;
import org.gbif.hadoop.compress.d2.zip.ZipEntry;
import org.gbif.occurrence.download.hive.DownloadTerms;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateEmlMetadata;
import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateRdf;

/**
 * Utility class to export GBIF snapshots into DataOne.
 */
class DownloadsExport {

  private final Config config;

  private final DataPackageManager dataPackageManager;

  private final FileSystem fileSystem;

  private static final String HIVE_EXPORT_SCRIPT = "export_snapshot.ql";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Logger LOG = LoggerFactory.getLogger(DownloadsExport.class);

  static {
    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private DownloadsExport(Config config) {
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
  private DataPackage createEmlMetadata(Collection<Term> terms, String exportFileName, Long fileSize, String doi) {
    try {
      File file =
        generateEmlMetadata(terms, config.getSnapshotTable(), exportFileName, fileSize, getSnapshotRecordCount(), doi);
      DataPackage dataPackageCreated = dataPackageManager.createSnapshotEmlDataPackage(file, doi);
      //The file can be deleted since it was copied into the DataRepo
      file.delete();
      return dataPackageCreated;
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  private DataPackage createEmlMetadata(Collection<Term> terms, Path exportFile, String doi) {
    try {
      return createEmlMetadata(terms, exportFile.getName(), fileSystem.getStatus(exportFile).getCapacity(), doi);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  /**
   * Creates the RDF metadata for the snapshot table and persists it into the DataRepo.
   */

  private void createRdf(String doi, UUID dataObjectId, UUID emlId) {
    try {
      UUID rdfId = UUID.randomUUID();
      File file = generateRdf(config.getSnapshotTable(), dataObjectId, emlId, rdfId);
      dataPackageManager.createSnapshotRdfDataPackage(file, doi, rdfId);
      //The file can be deleted since it was copied into the DataRepo
      file.delete();
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  /**
   * Executes a record count of the snapshot table using Hive JDBC.
   */
  private Long getSnapshotRecordCount() {
    try {
      Class.forName("org.apache.hive.jdbc.HiveDriver");
    } catch (ClassNotFoundException ex) {
      throw Throwables.propagate(ex);
    }

    try (Connection con = DriverManager.getConnection(config.getHive2JdbcUrl(), "hive", "");
         Statement stmt = con.createStatement(); ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM "
                                                                                      + config.getHiveDB()
                                                                                      + "."
                                                                                      + config.getSnapshotTable())) {
      return result.next() ? result.getLong(1) : 0;
    } catch (SQLException ex) {
      throw Throwables.propagate(ex);
    }
  }

  /**
   * Export process: creates a zip file of the snapshot table, an EML metadata file and a RDF description.
   */
  private void run() {
    // TODO: get latest download and DOI from WS. For now we set it manually in the config

    for (Config.DownloadInfo downloadInfo : config.getDownloads()) {
      Path exportPath = new Path(downloadInfo.path);
      LOG.info("Creating a data package for the CSV download");
      DataPackage dataPackageData =
        dataPackageManager.createSnapshotDataPackageFromDownload(fileSystem.getUri().resolve(exportPath.toUri()),
                                                                 downloadInfo.doi);
      LOG.info("Deleting temporary files");
      LOG.info("Creating EML metadata");
      DataPackage dataPackageEml =
        createEmlMetadata(getDownloadFields(downloadInfo.format), exportPath, dataPackageData.getDoi().toString());
      LOG.info("Creating RDF metadata");
      createRdf(dataPackageData.getDoi().toString(), dataPackageData.getKey(), dataPackageEml.getKey());
    }
  }

  Set<Term> getDownloadFields(Config.Format format) {
    if (format == Config.Format.DWCA) {
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
    Config config = objectMapper.readValue(new File(arg[0]), Config.class);
    new DownloadsExport(config).run();
  }

}
