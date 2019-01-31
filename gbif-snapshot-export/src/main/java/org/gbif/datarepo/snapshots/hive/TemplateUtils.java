package org.gbif.datarepo.snapshots.hive;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.google.common.base.Throwables;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

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
        try (Writer writer = Files.newBufferedWriter(Paths.get(exportPath), StandardCharsets.UTF_8)) {
            Template eml = cfg.getTemplate(templateFile);
            eml.process(params, writer);
        } catch (TemplateException | IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
