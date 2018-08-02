package org.gbif.datarepo.snapshots.hive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.inject.DataRepoFsModule;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.hadoop.compress.d2.D2CombineInputStream;
import org.gbif.hadoop.compress.d2.D2Utils;
import org.gbif.hadoop.compress.d2.zip.ModalZipOutputStream;
import org.gbif.hadoop.compress.d2.zip.ZipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateEmlMetadata;
import static org.gbif.datarepo.snapshots.hive.MetadataGenerator.generateRdf;

class SnapshotExport {

    private final Config config;

    private final DataPackageManager dataPackageManager;

    private final FileSystem fileSystem;

    private static final String HIVE_EXPORT_SCRIPT = "export_snapshot.ql";

    private static final ObjectMapper OBJECT_MAPPER  = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotExport.class);

    static {
        // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }


    private SnapshotExport(Config config) {
        this.config = config;
        DataRepoFsModule dataRepoFsModule = new DataRepoFsModule(config.getDataRepoConfiguration(), null, null);
        dataPackageManager = new DataPackageManager(dataRepoFsModule.dataRepository(OBJECT_MAPPER));
        fileSystem = initFileSystem();
    }

    /**
     * Runs the generated Hive script HIVE_EXPORT_SCRIPT that exports a snapshot to a compressed table.
     */
    private static void runHiveExportQuery() {
        try {
            new ProcessBuilder("hive" , "-f", HIVE_EXPORT_SCRIPT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor();
        } catch (IOException | InterruptedException ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Extracts the JAR file name of the running app, this later is used to add it as JAR in the Hive script.
     * This is required in order to have the hadoop-compress in the hadoop cache.
     */
    private static String getRunningContext() {
        return new java.io.File(SnapshotExport.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
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
     * Merges the pre-deflated content using the hadoop-compress library.
     */
    private void zipPreDeflated(String header, Path sourcePath, Path outputPath) throws IOException {
        appendHeaderFile(header, sourcePath);
        try (FSDataOutputStream zipped = fileSystem.create(outputPath, true);
             ModalZipOutputStream zos = new ModalZipOutputStream(new BufferedOutputStream(zipped));
             D2CombineInputStream in =
                     new D2CombineInputStream(
                             Arrays.stream(fileSystem.listStatus(sourcePath))
                                     .map(
                                             input -> {
                                                 try {
                                                     return fileSystem.open(input.getPath());
                                                 } catch (IOException ex) {
                                                     throw Throwables.propagate(ex);
                                                 }
                                             })
                                     .collect(Collectors.toList()))
        )
        {

            ZipEntry ze = new ZipEntry(sourcePath.getName() + ".csv");
            zos.putNextEntry(ze, ModalZipOutputStream.MODE.PRE_DEFLATED);
            ByteStreams.copy(in, zos);
            in.close(); // required to get the sizes
            ze.setSize(in.getUncompressedLength()); // important to set the sizes and CRC
            ze.setCompressedSize(in.getCompressedLength());
            ze.setCrc(in.getCrc32());
            zos.closeEntry();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }


    /**
     * Creates a compressed file named '0' that contains the content of the file HEADER.
     */
    private void appendHeaderFile(String header, Path dir) throws IOException {
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(new Path(dir, "0"))) {
            D2Utils.compress(new ByteArrayInputStream(header.getBytes()), fsDataOutputStream);
        }
    }


    /**
     * This method encapsulates all the Hive processing.
     */
    private Path runHiveExport(Map<Term, FieldSchema> colTerms) throws IOException {
        Map<String,String> hiveColMapping = colTerms.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != GbifTerm.gbifID)
                .collect(Collectors.toMap(e -> e.getKey().simpleName(), e -> e.getValue().getName(), (e1,e2) -> e1, TreeMap::new));
        Map<String, Object> params = new HashMap<>();
        params.put("colMap", hiveColMapping);
        params.put("hiveDB", config.getHiveDB());
        params.put("snapshotTable", config.getSnapshotTable());
        String runningContext = getRunningContext();
        if (runningContext.endsWith(".jar")) {
            params.put("thisJar", runningContext);
        }
        TemplateUtils.runTemplate(params, "export_snapshot.ftl", HIVE_EXPORT_SCRIPT);
        String header = GbifTerm.gbifID.simpleName() + '\t' +colTerms.keySet().stream().filter(term -> term != GbifTerm.gbifID)
                .map(Term::simpleName).sorted().collect(Collectors.joining("\t"))  + '\n';
        runHiveExportQuery();
        Path exportPath = new Path(config.getExportPath() + config.getSnapshotTable()  + "csv.zip");
        zipPreDeflated(header, new Path("/user/hive/warehouse/" + config.getHiveDB() + ".db/export_" + config.getSnapshotTable() + "/"), exportPath);
        return exportPath;
    }

    /**
     * Gets the correspondent Term for the column name.
     */
    private static Term getColumnTerm(String columnName) {
        if (columnName.equalsIgnoreCase("id")) {
            return GbifTerm.gbifID;
        }
        if (columnName.equalsIgnoreCase("publisher_country")) {
            return GbifTerm.publishingCountry;
        }
        if (columnName.equalsIgnoreCase("publisher_id")) {
            return GbifInternalTerm.publishingOrgKey;
        }
        return  TermFactory.instance()
                .findTerm(columnName.replaceFirst("v_", "")
                        .replaceAll("_id", "key").replaceAll("_",""));
    }

    /**
     * Creates the EML metadata for the snapshot table and persists it into the DataRepo.
     */
    private void createEmlMetadata(Collection<Term> terms, Path exportFile, String doi) {
        try {
            File file = generateEmlMetadata(terms, config.getSnapshotTable(), exportFile.getName(),
                    fileSystem.getStatus(exportFile).getCapacity(), getSnapshotRecordCount(), doi);
            dataPackageManager.createSnapshotEmlDataPackage(file,  doi);
            //The file can be deleted since it was copied into the DataRepo
            file.delete();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Creates the RDF metadata for the snapshot table and persists it into the DataRepo.
     */

    private void createRdf(String doi) {
        try {
            File file  = generateRdf(config.getSnapshotTable());
            dataPackageManager.createSnapshotRdfDataPackage(file, doi);
            //The file can be deleted since it was copied into the DataRepo
            file.delete();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Contacts the Hive Metastore API to get column names.
     */
    private Map<Term,FieldSchema> getTermsHiveColumnMapping() {
        HiveMetaStoreClient hiveMetaStoreClient = null;
        try {
            HiveConf hiveConf = new HiveConf();
            hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.getMetaStoreUris());
            hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            return hiveMetaStoreClient.getFields(config.getHiveDB(), config.getSnapshotTable())
                    .stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(getColumnTerm(fieldSchema.getName()), fieldSchema))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        } catch (TException ex) {
            throw Throwables.propagate(ex);
        } finally{
            Optional.ofNullable(hiveMetaStoreClient).ifPresent(HiveMetaStoreClient::close);
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
             Statement stmt = con.createStatement();
             ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM " + config.getHiveDB() + "." + config.getSnapshotTable())) {
            return result.next() ? result.getLong(1) : 0;
        } catch (SQLException ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Export process: creates a zip file of the snapshot table, an EML metadata file and a RDF description.
     */
    private void run() {
        try {
            Map<Term,FieldSchema>  colTerms = getTermsHiveColumnMapping();
            LOG.info("Exporting snapshot table into a compressed table");
            Path exportPath = runHiveExport(colTerms);
            LOG.info("Creating a data package for the GBIF snapshot");
            DataPackage dataPackage = dataPackageManager.createSnapshotDataPackage(fileSystem.getUri().resolve(exportPath.toUri()));
            LOG.info("Deleting temporary files");
            fileSystem.delete(exportPath, false);
            LOG.info("Creating EML metadata");
            createEmlMetadata(colTerms.keySet(), exportPath, dataPackage.getDoi().toString());
            LOG.info("Creating RDF metadata");
            createRdf(dataPackage.getDoi().toString());
        } catch (IOException ex) {
            LOG.error("Error exporting snapshot as data package", ex);
            throw Throwables.propagate(ex);
        }
    }


    /**
     * Runs the export process using a configuration YAML file as parameter.
     */
    public static void main(String[] arg) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Config config = objectMapper.readValue(new File(arg[0]), Config.class);
        new SnapshotExport(config).run();
    }

}
