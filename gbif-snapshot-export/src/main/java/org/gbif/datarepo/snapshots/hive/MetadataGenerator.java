package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.File;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that creates the EML and RDF elements for the GBIF Snapshot.
 */
class MetadataGenerator {

    //Date format used in table names
    private static final String SNAPSHOT_TABLE_DATE_FORMAT = "yyyyMMdd";

    //Date format used for the generated metadata
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String ENCODING = "UTF-8";

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
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        if (components.length > 1) {
          try {
              return dateFormat.format(new SimpleDateFormat(SNAPSHOT_TABLE_DATE_FORMAT).parse(components[1]));
          } catch (ParseException ex) {
            return dateFormat.format(new Date());
          }
        }
        return dateFormat.format(new Date());
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
    static File generateRdf(String snapshotTable, UUID dataObjectId, UUID emlId, UUID rdfId) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("exportDate", exportDate(snapshotTable));
            params.put("URL_ENCODED_ORE_GUID", URLEncoder.encode(rdfId.toString(), ENCODING));
            params.put("ORE_GUID", rdfId.toString());
            params.put("URL_ENCODED_METADATA_GUID", URLEncoder.encode(emlId.toString(), ENCODING));
            params.put("METADATA_GUID", emlId.toString());
            params.put("URL_ENCODED_DATA_GUID", URLEncoder.encode(dataObjectId.toString(), ENCODING));
            params.put("DATA_GUID", dataObjectId.toString());
            TemplateUtils.runTemplate(params, "rdf.ftl", snapshotTable + ".rdf");
            return new File(snapshotTable + ".rdf");
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

}
