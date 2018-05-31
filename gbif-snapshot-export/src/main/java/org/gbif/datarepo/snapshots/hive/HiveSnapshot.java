package org.gbif.datarepo.snapshots.hive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.inject.DataRepoFsModule;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.hadoop.compress.d2.D2CombineInputStream;
import org.gbif.hadoop.compress.d2.D2Utils;
import org.gbif.hadoop.compress.d2.zip.ModalZipOutputStream;
import org.gbif.hadoop.compress.d2.zip.ZipEntry;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

class HiveSnapshot {

    private final Config config;

    private final DataRepository dataRepository;

    private static final ObjectMapper OBJECT_MAPPER  = new ObjectMapper();

    static {
        // determines whether encountering from unknown properties (ones that do not map to a property, and there is no
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // "any setter" or handler that can handle it) should result in a failure (throwing a JsonMappingException) or not.
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }


    HiveSnapshot(Config config) {
        this.config = config;
        DataRepoFsModule dataRepoFsModule = new DataRepoFsModule(config.getDataRepoConfiguration(), null, null);

        dataRepository = dataRepoFsModule.dataRepository(new ObjectMapper());
    }

    private static int runHiveExport(String pathToQueryFile) {
        try {
            return new ProcessBuilder("hive" , "-f", pathToQueryFile)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor();
        } catch (IOException | InterruptedException ex) {
            throw  new RuntimeException(ex);
        }
    }

    private static FileSystem getFileSystem() {
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
        FileSystem fs = getFileSystem();
        appendHeaderFile(header, fs, sourcePath);
        try (FSDataOutputStream zipped = fs.create(outputPath, true);
             ModalZipOutputStream zos = new ModalZipOutputStream(new BufferedOutputStream(zipped));
             D2CombineInputStream in =
                     new D2CombineInputStream(
                             Arrays.stream(fs.listStatus(sourcePath))
                                     .map(
                                             input -> {
                                                 try {
                                                     return fs.open(input.getPath());
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
    private void appendHeaderFile(String header, FileSystem fileSystem, Path dir)
            throws IOException {
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(new Path(dir, "0"))) {
            D2Utils.compress(new ByteArrayInputStream(header.getBytes()), fsDataOutputStream);
        }
    }

    private static String getRunningContext() {
        return new java.io.File(HiveSnapshot.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private void generateHiveExport(Map<Term, FieldSchema> colTerms) {
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
        TemplateUtils.runTemplate(params, "export_snapshot.ftl", "export_snapshot.ql");
    }



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

    private void generateEmlMetadata(Collection<Term> terms, Path exportFile, String doi) {
        try {
        Map<String,Object> params = new HashMap<>();
        params.put("terms", terms.stream().filter(t -> GbifTerm.gbifID != t).sorted(Comparator.comparing(Term::simpleName)).collect(Collectors.toList()));
        params.put("exportFileName",exportFile.getName());
        params.put("exportFileSize",getFileSystem().getStatus(exportFile).getCapacity());
        params.put("doi", doi);
        params.put("numberOfRecords", count());
        params.put("exportDate", SimpleDateFormat.getDateTimeInstance().format(new Date()));
        File file = new File(config.getSnapshotTable() + ".eml");
        TemplateUtils.runTemplate(params, "eml.ftl", file.getName());
        createDataPackage(file, "GBIF Snapshot Metadata " + file.getName(),
                "EML Metadata for GBIF Snapshot " + file.getName(), doi);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void generateRdf(String doi) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("snapshotTable", config.getSnapshotTable());
            params.put("exportDate", SimpleDateFormat.getDateTimeInstance().format(new Date()));
            TemplateUtils.runTemplate(params, "rdf.ftl", config.getSnapshotTable() + ".rdf");
            File file = new File(config.getSnapshotTable() + ".rdf");
            createDataPackage(file, "GBIF Snapshot RDF Metadata " + file.getName(),
                    "RDF Metadata for GBIF Snapshot " + file.getName(), doi);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private DataPackage createDataPackage(String title, String description, String doi) {
        DataPackage dataPackage = createDataPackage(title, description);
        Identifier identifier = new Identifier();
        identifier.setIdentifier(doi);
        identifier.setType(Identifier.Type.DOI);
        identifier.setRelationType(Identifier.RelationType.References);
        identifier.setCreatedBy("gbif-snapshot");
        dataPackage.addRelatedIdentifier(identifier);
        return dataPackage;
    }

    private DataPackage createDataPackage(String title, String description) {
        DataPackage dataPackage = new DataPackage();
        dataPackage.setCreatedBy("gbif-snapshot");
        dataPackage.setTitle(title);
        dataPackage.setDescription(description);
        dataPackage.setLicense(License.CC_BY_4_0);
        dataPackage.setShareIn(Sets.newHashSet("DataOne"));

        return dataPackage;
    }

    private DataPackage createDataPackage(Path file, String title, String description) {
        try (InputStream inputStream = getFileSystem().open(file)){
            return dataRepository.create(createDataPackage(title, description),
                    Collections.singletonList(FileInputContent.from(file.getName(), inputStream)), true);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private DataPackage createDataPackage(File file, String title, String description, String doi) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return dataRepository.create(createDataPackage(title, description, doi),
                    Collections.singletonList(FileInputContent.from(file.getName(), inputStream)), true);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void export() {
        try {
            HiveConf hiveConf = new HiveConf();
            hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.getMetaStoreUris());
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            Map<Term,FieldSchema> colTerms =  hiveMetaStoreClient.getFields(config.getHiveDB(), config.getSnapshotTable())
                    .stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(getColumnTerm(fieldSchema.getName()), fieldSchema))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
            String header = GbifTerm.gbifID.simpleName() + '\t' +colTerms.keySet().stream().filter(term -> term != GbifTerm.gbifID)
                    .map(Term::simpleName).sorted().collect(Collectors.joining("\t"))  + '\n';
            generateHiveExport(colTerms);
            runHiveExport("export_snapshot.ql");
            Path exportPath = new Path(config.getExportPath() + config.getSnapshotTable()  + "csv.zip");
            zipPreDeflated(header, new Path("/user/hive/warehouse/" + config.getHiveDB() + ".db/export_" + config.getSnapshotTable() + "/"), exportPath);
            DataPackage dataPackage = createDataPackage(exportPath, "GBIF occurrence data snapshot " + exportPath.getName(), "GBIF snapshot data in compress format");
            generateEmlMetadata(colTerms.keySet(), exportPath, dataPackage.getDoi().toString());
            generateRdf(dataPackage.getDoi().toString());
        } catch (TException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Long count() {
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (Connection con = DriverManager.getConnection(config.getHive2JdbcUrl(), "hive", "");
             Statement stmt = con.createStatement();
             ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM " + config.getHiveDB() + "." + config.getSnapshotTable())) {
            return result.next() ? result.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
