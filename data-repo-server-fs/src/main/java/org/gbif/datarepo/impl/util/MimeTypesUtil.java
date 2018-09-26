package org.gbif.datarepo.impl.util;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;


import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to detect and handle Mime Types.
 */
public class MimeTypesUtil {

    private static final Detector TIKA_DETECTOR = new AutoDetectParser().getDetector();
    // mime types
    private static final String RDF_MIME_TYPE = "application/rdf+xml";
    private static final String EML_MIME_TYPE = "message/rfc822";
    // dataone formats
    private static final String RDF_DATAONE_FORMAT = "http://www.openarchives.org/ore/terms";
    private static final String EML_DATAONE_FORMAT = "eml://ecoinformatics.org/eml-2.1.1";

    /**
     * Utility classes must have private constructors.
     */
    private MimeTypesUtil() {
        //do nothing
    }

    /**
     * Detects the MimeType of a file.
     * If the content type can be not be detected thru the file name, the file is opened and analyze to
     * detect its content type.
     *
     * @param fileName input file name
     * @return the detected MimeType, 'application/octet-stream' if it is not detected
     * @throws IOException if an error occurs reading the input stream
     */
    public static String detectMimeType(String fileName) {
        try {
            if (!com.google.common.io.Files.getFileExtension(fileName).isEmpty()) {
                return detectMimeType(null, fileName);
            }
            try (InputStream is = new FileInputStream(fileName); BufferedInputStream bis = new BufferedInputStream(is)) {
                return detectMimeType(bis, fileName);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Detects the MimeType of a file and input stream
     *
     * @param is       file content as an input stream, if null, the fileName is used to detect the MimeType
     * @param fileName input file name
     * @return the detected MimeType, 'application/octet-stream' in cases when the content is not detected
     * @throws IOException if an error occurs reading the input stream
     */
    public static String detectMimeType(@Nullable InputStream is, String fileName) throws IOException {
        Metadata md = new Metadata();
        md.add(Metadata.RESOURCE_NAME_KEY, fileName);
        //we can return here since this method return 'application/octet-stream' if no other type was detected
        return TIKA_DETECTOR.detect(is, md).toString();
    }

    /**
     * Detects the format id suitable for DataOne.
     *
     * @param fileName input file name
     * @return the detected format id.
     */
    public static String detectDataOneFormat(String fileName) {
        String mimeType = detectMimeType(fileName);

        if (RDF_MIME_TYPE.equals(mimeType)) {
            return RDF_DATAONE_FORMAT;
        }

        if (EML_MIME_TYPE.equals(mimeType)) {
            return EML_DATAONE_FORMAT;
        }

        return mimeType;
    }

    /**
     * Converts a format id to its corresponding mime type.
     *
     * @param formatId format id to convert
     * @return mime type asscoiated to the format id
     */
    public static String convertFormatIdToMimetype(String formatId) {
        if (RDF_DATAONE_FORMAT.equals(formatId)) {
            return RDF_MIME_TYPE;
        }

        if (EML_DATAONE_FORMAT.equals(formatId)) {
            return EML_MIME_TYPE;
        }

        return formatId;
    }
}
