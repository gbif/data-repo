package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import freemarker.template.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class to run freemarker templates.
 */
class TemplateUtils {

    //Templates location
    private static final String TEMPLATES_DIR = "/templates/";

    /**
     * Private constructor.
     */
    private TemplateUtils() {
        //DO NOTHING
    }

    /**
     * Runs a FreeMarker template using a map of parameters. The output is generated into exportPath.
     */
    static void runTemplate(Map<?,?> params, String templateFile, String exportPath) {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        // Where do we load the templates from:
        cfg.setClassForTemplateLoading(TemplateUtils.class, TEMPLATES_DIR);
        // Some other recommended settings:
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        // Write output to the console
        try (Writer writer = new FileWriter(new File(exportPath))) {
            Template eml = cfg.getTemplate(templateFile);
            eml.process(params, writer);
        } catch (TemplateException | IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
