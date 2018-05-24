package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Template;
import freemarker.template.Version;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
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
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class HiveSnapshotExport {

    public static class Config {
        private final String metaStoreUris;
        private final String hive2JdbcUrl;
        private final String hiveDB;
        private final String snapshotTable;

        public Config(String metaStoreUris, String hive2JdbcUrl, String hiveDB, String snapshotTable) {
            this.metaStoreUris = metaStoreUris;
            this.hive2JdbcUrl = hive2JdbcUrl;
            this.hiveDB = hiveDB;
            this.snapshotTable = snapshotTable;
        }

        public String getMetaStoreUris() {
            return metaStoreUris;
        }

        public String getHiveDB() {
            return hiveDB;
        }

        public String getSnapshotTable() {
            return snapshotTable;
        }

        public String getHive2JdbcUrl() {
            return hive2JdbcUrl;
        }


        public String getFullSnapshotTableName() {
            return hiveDB  + "." + snapshotTable;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(HiveSnapshotExport.class);
    private static final String TEMPLATES_DIR = "/templates/";
    private final Config config;

    public HiveSnapshotExport(Config config) {
        this.config = config;
    }

    private void generateEml(Map<String, Term> colTerms) throws IOException {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        // Where do we load the templates from:
        cfg.setClassForTemplateLoading(HiveSnapshotExport.class, "/templates/");
        // Some other recommended settings:
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template eml = cfg.getTemplate("eml.ftl");
        Map<String, Object> input = new HashMap<>();
        input.put("terms", colTerms.values());
        input.put("exportDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        input.put("exportFileName",config.snapshotTable + ".gz");
        input.put("doi", "");
        input.put("exportFileSize","");
        input.put("numberOfRecords", "");
        // Write output to the console
        Writer consoleWriter = new OutputStreamWriter(System.out);
        try {
        eml.process(input, consoleWriter);
        } catch (TemplateException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getRunningContext() {
        return new java.io.File(HiveSnapshotExport.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private void runTemplate(Map<?,?> params, String templateFile, String exportPath) {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        // Where do we load the templates from:
        cfg.setClassForTemplateLoading(HiveSnapshotExport.class, TEMPLATES_DIR);
        // Some other recommended settings:
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        // Write output to the console
        try (Writer writer = new FileWriter(new File(exportPath))) {
            Template eml = cfg.getTemplate(templateFile);
            eml.process(params, writer);
        } catch (TemplateException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void generateHiveExport(Map<FieldSchema, Term> colTerms) throws IOException {
        Map<String,String> hiveColMapping = colTerms.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(e -> toHiveColumn(e.getKey()), e -> e.getValue().simpleName()));
        Map<String, Object> params = new HashMap<>();
        params.put("colMap", hiveColMapping);
        params.put("hiveDB", config.getHiveDB());
        params.put("snapshotTable", config.getSnapshotTable());
        String runningContext = getRunningContext();
        if(runningContext.endsWith(".jar")) {
           params.put("thisJar", runningContext);
        }
        runTemplate(params, "export_snapshot.ftl", "export_snapshot.ql");
    }

    private int runHiveExport(String pathToQueryFile) {
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
    private static String toHiveColumn(FieldSchema field) {
      return field.getType().equals("string") ? "cleanDelimiters("  + field.getName()+ ")" : field.getName();
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
             return result.next() ? result.getLong(1): 0    ;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void export() {
        try {
            HiveConf hiveConf = new HiveConf();
            hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.getMetaStoreUris());
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            Map<FieldSchema,Term> colTerms =  hiveMetaStoreClient.getFields(config.hiveDB, config.snapshotTable)
                    .stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(fieldSchema, getColumnTerm(fieldSchema.getName())))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
            String header = colTerms.values().stream().map(Term::simpleName).collect(Collectors.joining("\t"));
            generateHiveExport(colTerms);
            runHiveExport("export_snapshot.ql");
            zipPreDeflated(header, new Path("/user/hive/warehouse/" + config.getHiveDB() + ".db/export_" + config.getSnapshotTable() + "/"), new Path("/tmp/" + config.getSnapshotTable()  + ".zip"));
        } catch (TException | IOException ex) {
          throw new RuntimeException(ex);
        }
    }

    private static Term getColumnTerm(String columnName) {
        if (columnName.equalsIgnoreCase("dataset_id")) {
            return GbifTerm.datasetKey;
        }
        if (columnName.equalsIgnoreCase("id")) {
            return GbifTerm.gbifID;
        }
        return  TermFactory.instance()
                .findTerm(columnName.replaceFirst("v_", ""));
    }

    public FileSystem getFileSystem() {
        try {
            org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
            configuration.addResource("hdfs-site.xml");
            return FileSystem.newInstance(configuration);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

    }

    /**
     * Merges the pre-deflated content using the hadoop-compress library.
     */
    private void zipPreDeflated(String header, Path sourcePath, Path outputPath) throws IOException {
        FileSystem fs = getFileSystem();
        LOG.info("Zipping {} to {} in FileSystem", sourcePath, outputPath, fs.getUri());
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


    //private HiveMetaStoreClient hiveMetaStoreClient;

    public static void main(String[] arg) throws Exception {
        Config config = new Config("thrift://c5master1-vh.gbif.org:9083","jdbc:hive2://c5master2-vh.gbif.org:10000/",
                "fede","raw_20180409_small");
        HiveSnapshotExport export = new HiveSnapshotExport(config);
        export.export();
    }

}
