package org.gbif.datarepo.snapshots.hive;

import com.google.common.collect.Maps;
import freemarker.template.*;
import org.apache.hadoop.hive.cli.CliSessionState;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.CommandNeedRetryException;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.hadoop.hive.ql.session.SessionState;
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
        runTemplate(Collections.singletonMap("colMap", hiveColMapping), "export_snapshot.ftl", "export_snapshot.ql");
    }

    private static String toHiveColumn(FieldSchema field) {
      return field.getType().equals("string") ? "cleanDelimiters("  + field.getName()+ ")" : field.getName();
    }

    public void export() {
        try {
            HiveConf hiveConf = new HiveConf();
            //hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.metaStoreUris);
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
           // org.apache.hadoop.hive.ql.Driver driver = new Driver(hiveConf);
           // SessionState.start(new CliSessionState(hiveConf));
           // CommandProcessorResponse response = driver.run("select count (*) from " + config.getFullSnapshotTableName());

            Map<FieldSchema,Term> colTerms =  hiveMetaStoreClient.getFields(config.hiveDB, config.snapshotTable).stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(fieldSchema,TermFactory.instance()
                            .findTerm(fieldSchema.getName().replaceFirst("v_", ""))))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            generateHiveExport(colTerms);
        } catch (TException | IOException ex) {
          throw new RuntimeException(ex);
        }
    }


    //private HiveMetaStoreClient hiveMetaStoreClient;

    public static void main(String[] arg) throws Exception {
        Config config = new Config("thrift://c5master1-vh.gbif.org:9083","jdbc:hive2://c5master2-vh.gbif.org:10000/",
                "snapshot","raw_20180409" );
        HiveSnapshotExport export = new HiveSnapshotExport(config);
        export.export();
    }

}
