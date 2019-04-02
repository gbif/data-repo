package org.gbif.datarepo.snapshots.datapackage;

import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.inject.DataRepoFsModule;
import org.gbif.datarepo.snapshots.DataPackageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class generate metadata from data packages.
 */
class DataPackageMetadataGenerator {

  private final DataPackageConfig config;

  private final DataPackageManager dataPackageManager;

  private final FileSystem fileSystem;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Logger LOG = LoggerFactory.getLogger(DataPackageMetadataGenerator.class);

  static {
    // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
    OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private DataPackageMetadataGenerator(DataPackageConfig config) {
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
   * Export process: creates a zip file of the snapshot table, an EML metadata file and a RDF description.
   */
  private void run() {
    for(UUID dataPackageKey : config.getPackageKeys()) {
      DataPackage dp = dataPackageManager.getDataPackage(dataPackageKey);

      // get file name. There should be only 1 file
      dp.getFiles().stream().findFirst().ifPresent(dataPackageFile -> {
        InputStream is = dataPackageManager.getDataPackageInputStream(dataPackageKey, dataPackageFile.getFileName());
        try {
          String checksum = DigestUtils.md5Hex(is);
          LOG.info("Data package {} with checksum {}", dataPackageKey, checksum);
        } catch (IOException e) {
          LOG.error("Could not generate checksum for data package {}", dataPackageKey, e);
        }
      });
    }
  }

  /**
   * Runs the export process using a configuration YAML file as parameter.
   */
  public static void main(String[] arg) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    DataPackageConfig config = objectMapper.readValue(new File(arg[0]), DataPackageConfig.class);
    new DataPackageMetadataGenerator(config).run();
  }

}
