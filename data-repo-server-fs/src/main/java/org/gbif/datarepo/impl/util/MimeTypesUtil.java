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

    /**
     * Utility classes must have private constructors.
     */
    private MimeTypesUtil() {
      //do nothing
    }

    /**
     *  Detects the MimeType of a file.
     *  If the content type can be not be detected thru the file name, the file is opened and analyze to
     *  detect its content type.
     * @param fileName  input file name
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
     *  Detects the MimeType of a file and input stream
     * @param is file content as an input stream, if null, the fileName is used to detect the MimeType
     * @param fileName  input file name
     * @return the detected MimeType, 'application/octet-stream' in cases when the content is not detected
     * @throws IOException if an error occurs reading the input stream
     */
    public static String detectMimeType(@Nullable InputStream is, String fileName) throws IOException {
        Metadata md = new Metadata();
        md.add(Metadata.RESOURCE_NAME_KEY, fileName);
        //we can return here since this method return 'application/octet-stream' if no other type was detected
        return TIKA_DETECTOR.detect(is, md).toString();
    }
}
