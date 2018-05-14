package org.gbif.datarepo.snapshots.hive;

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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Config config;

    public HiveSnapshotExport(Config config) {
        this.config = config;
    }

    private void generateEml(List<Term> termFields) throws IOException {
        Configuration cfg = new Configuration(new Version(2, 3, 23));
        // Where do we load the templates from:
        cfg.setClassForTemplateLoading(HiveSnapshotExport.class, "/templates/");
        // Some other recommended settings:
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template eml = cfg.getTemplate("eml.ftl");
        Map<String, Object> input = new HashMap<>();
        input.put("terms", termFields);
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

    public void export() {
        try {
            HiveConf hiveConf = new HiveConf();
            //hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.metaStoreUris);
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            org.apache.hadoop.hive.ql.Driver driver = new Driver(hiveConf);
            HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);
            SessionState.start(new CliSessionState(hiveConf));
            CommandProcessorResponse response = driver.run("select count (*) from " + config.getFullSnapshotTableName());

            List<FieldSchema> fieldSchemas = hiveMetaStoreClient.getFields(config.hiveDB, config.snapshotTable);
            generateEml(fieldSchemas.stream().map(fieldSchema -> TermFactory.instance()
                    .findTerm(fieldSchema.getName().replaceFirst("v_", "")))
                .collect(Collectors.toList()));

        } catch (TException | IOException | CommandNeedRetryException ex) {
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
