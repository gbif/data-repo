package org.gbif.datarepo.snapshots.hive;

import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.io.File;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;

/**
 * Utility class that creates the EML and RDF elements for the GBIF Snapshot.
 */
class MetadataGenerator {

    private static final String ENCODING = "UTF-8";

    //Date format used in table names
    private static final String SNAPSHOT_TABLE_DATE_FORMAT = "yyyyMMdd";

    //Date format used for the generated metadata
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String PREFIX_METADATA_FILES = "occurrence-";

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
    private static String exportDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        try {
            return dateFormat.format(new SimpleDateFormat(SNAPSHOT_TABLE_DATE_FORMAT).parse(date));
        } catch (ParseException ex) {
            return dateFormat.format(new Date());
        }
    }

    /**
     * Executes a FreeMarker template that generates the EML metadata document.
     */
    static File generateEmlMetadata(Collection<Term> terms, String date, String fileName, Long fileSize, Long numberOfRecords,
                                     String doi) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("terms", terms.stream().filter(t -> GbifTerm.gbifID != t).sorted(Comparator.comparing(Term::simpleName)).collect(Collectors.toList()));
            params.put("exportFileName", fileName);
            params.put("exportFileSize", fileSize);
            params.put("doi", doi);
            params.put("numberOfRecords", numberOfRecords);
            params.put("exportDate", exportDate(date));
            File file = new File( PREFIX_METADATA_FILES + date + ".eml");
            TemplateUtils.runTemplate(params, "eml.ftl", file.getName());
            return file;
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Executes a FreeMarker template that generates the RDF document.
     */
    static File generateRdf(String date, UUID dataObjectId, UUID emlId, UUID rdfId) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("exportDate", exportDate(date));
            params.put("URL_ENCODED_ORE_GUID", URLEncoder.encode(rdfId.toString(), ENCODING));
            params.put("ORE_GUID", rdfId.toString());
            params.put("URL_ENCODED_METADATA_GUID", URLEncoder.encode(emlId.toString(), ENCODING));
            params.put("METADATA_GUID", emlId.toString());
            params.put("URL_ENCODED_DATA_GUID", URLEncoder.encode(dataObjectId.toString(), ENCODING));
            params.put("DATA_GUID", dataObjectId.toString());
            TemplateUtils.runTemplate(params, "rdf.ftl", PREFIX_METADATA_FILES + date + ".rdf");
            return new File(PREFIX_METADATA_FILES + date + ".rdf");
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

}
