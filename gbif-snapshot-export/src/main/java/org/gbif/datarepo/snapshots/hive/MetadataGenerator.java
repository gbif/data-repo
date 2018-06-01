package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that creates the EML and RDF elements for the GBIF Snapshot.
 */
class MetadataGenerator {

    private static final String SNAPSHOT_TABLE_DATE_FORMAT = "yyyyMMdd";

    /**
     * Private constructor.
     */
    private MetadataGenerator(){
        //DO NOTHING
    }

    /**
     * Tries to interpret the create date from the table name otherwise retuns the current date.
     * All GBIF Snapshot tables follow the pattern "occurrence_yyyyMMdd" as its table name.
     */
    private static String exportDate(String snapshotTable) {
        String[] components = snapshotTable.split("_");
        if (components.length > 1) {
          try {
            new SimpleDateFormat(SNAPSHOT_TABLE_DATE_FORMAT).parse(components[1]);
          } catch (ParseException ex) {
            return SimpleDateFormat.getDateTimeInstance().format(new Date());
          }
        }
        return SimpleDateFormat.getDateTimeInstance().format(new Date());
    }

    /**
     * Executes a FreeMarker template that generates the EML metadata document.
     */
    static File generateEmlMetadata(Collection<Term> terms, String snapshotTable, String fileName, Long fileSize, Long numberOfRecords,
                                     String doi) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("terms", terms.stream().filter(t -> GbifTerm.gbifID != t).sorted(Comparator.comparing(Term::simpleName)).collect(Collectors.toList()));
            params.put("exportFileName", fileName);
            params.put("exportFileSize", fileSize);
            params.put("doi", doi);
            params.put("numberOfRecords", numberOfRecords);
            params.put("exportDate", exportDate(snapshotTable));
            File file = new File(snapshotTable + ".eml");
            TemplateUtils.runTemplate(params, "eml.ftl", file.getName());
            return file;
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Executes a FreeMarker template that generates the RDF document.
     */
    static File generateRdf(String snapshotTable) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("snapshotTable", snapshotTable);
            params.put("exportDate", exportDate(snapshotTable));
            TemplateUtils.runTemplate(params, "rdf.ftl", snapshotTable + ".rdf");
            return new File(snapshotTable + ".rdf");
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
