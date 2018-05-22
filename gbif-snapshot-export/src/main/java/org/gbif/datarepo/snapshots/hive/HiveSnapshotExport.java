package org.gbif.datarepo.snapshots.hive;

import freemarker.template.*;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

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

    private void runHiveFile(String pathToFile) {
        try {
            Process process = Runtime.getRuntime().exec((new String[]{"hive", "-f", pathToFile}));
            process.waitFor();
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException(ex);
        }
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
        runTemplate(params, "export_snapshot.ftl", "export_snapshot.ql");
    }

    private int runHiveExport(String pathToQueryFile) {
        try {
            Process hiveScript = new ProcessBuilder("hive", "-f " + pathToQueryFile).start();
            return hiveScript.waitFor();
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
            hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.metaStoreUris);
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            Map<FieldSchema,Term> colTerms =  hiveMetaStoreClient.getFields(config.hiveDB, config.snapshotTable).stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(fieldSchema,TermFactory.instance()
                            .findTerm(fieldSchema.getName().replaceFirst("v_", ""))))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            generateHiveExport(colTerms);
            runHiveExport(new File("export_snapshot.ql").getAbsolutePath());
        } catch (TException | IOException ex) {
          throw new RuntimeException(ex);
        }
    }


    //private HiveMetaStoreClient hiveMetaStoreClient;

    public static void main(String[] arg) throws Exception {
        Config config = new Config("thrift://c5master1-vh.gbif.org:9083","jdbc:hive2://c5master2-vh.gbif.org:10000/",
                "fede","raw_20180409_small" );
        HiveSnapshotExport export = new HiveSnapshotExport(config);
        export.export();
    }

}
