package org.gbif.datarepo.datacite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Utility class to validate XML files against DataCite schemas.
 */
public class DataCiteSchemaValidator {

  private static final Logger LOG = LoggerFactory.getLogger(DataCiteSchemaValidator.class);

  private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  private static final Schema SCHEMA_31 = initSchema("https://schema.datacite.org/meta/kernel-3.1/metadata.xsd");
  private static final Schema SCHEMA_40 = initSchema("https://schema.datacite.org/meta/kernel-4.0/metadata.xsd");

  /**
   * Making constructor private.
   */
  private DataCiteSchemaValidator() {
    //empty constructor
  }

  /**
   * Initialize a Schema object from a URL.
   */
  private static Schema initSchema(String url) {
    try {
      return SCHEMA_FACTORY.newSchema(new URL(url));
    } catch (MalformedURLException | SAXException ex) {
      LOG.error("Error initiating schemas", ex);
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Validates a XML file against the supported DataCite schemas.
   */
  public static boolean isValidXML(File file) throws IOException {
    return isValidAgainstSchema(SCHEMA_31, file) || isValidAgainstSchema(SCHEMA_40, file);
  }

  /**
   * Validates an XML files against a schema.
   */
  private static boolean isValidAgainstSchema(Schema schema, File file) throws IOException {
    try (FileInputStream xmlInputStream = new FileInputStream(file)) {
      schema.newValidator().validate(new StreamSource(xmlInputStream));
      return true;
    } catch (SAXException ex) {
      LOG.warn("File {} is not valid against Schema {}", file.getName(), schema, ex);
      return false;
    }
  }
}
