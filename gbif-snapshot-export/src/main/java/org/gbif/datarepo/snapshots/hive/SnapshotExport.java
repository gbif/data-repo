package org.gbif.datarepo.snapshots.hive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Template;
import freemarker.template.Version;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class SnapshotExport {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotExport.class);
    private final Config config;

    public SnapshotExport(Config config) {
        this.config = config;
    }

    private void generateEml(Map<String, Term> colTerms) throws IOException {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        // Where do we load the templates from:
        cfg.setClassForTemplateLoading(SnapshotExport.class, "/templates/");
        // Some other recommended settings:
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template eml = cfg.getTemplate("eml.ftl");
        Map<String, Object> input = new HashMap<>();
        input.put("terms", colTerms.values());
        input.put("exportDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        input.put("exportFileName", config.getSnapshotTable() + ".gz");
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


    public void export() {
        new HiveSnapshot(config).export();
    }



    //private HiveMetaStoreClient hiveMetaStoreClient;

    public static void main(String[] arg) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Config config = objectMapper.readValue(new File(arg[0]), Config.class);
        SnapshotExport export = new SnapshotExport(config);
        export.export();
    }

}
