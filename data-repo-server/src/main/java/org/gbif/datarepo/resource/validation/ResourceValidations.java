package org.gbif.datarepo.resource.validation;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.datacite.DataCiteSchemaValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.assertj.core.util.Strings;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

public class ResourceValidations {


  public static List<FormDataBodyPart> validateFiles(FormDataMultiPart multiPart) {
    List<FormDataBodyPart> files = multiPart.getFields("file");
    if (files == null || files.isEmpty()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                          .entity("Data package must contain at least 1 file").build());
    }
    return files;
  }

  public static InputStream validateMetadata(FormDataMultiPart multiPart) throws IOException {
    FormDataBodyPart formDataBodyPart = multiPart.getField("metadata");
    if (formDataBodyPart == null) {
      throw throwBadRequest("Metadata file is required");
    }
    InputStream metadataStream = formDataBodyPart.getValueAs(InputStream.class);
    if (!DataCiteSchemaValidator.isValidXML(metadataStream)) {
      throwBadRequest("Invalid metadata provided");
    }
    return metadataStream;
  }

  public static DOI validateDoi(String doiRef) {
    if (Strings.isNullOrEmpty(doiRef)) {
      throwBadRequest("A non-empty DOI must be provided");
    }
    String[] doiParts = doiRef.split("-");
    if (doiParts.length != 2) {
      throwBadRequest(String.format("DOI format invalid %s, it must be in the format prefix-suffix", doiRef));
    }
    return new DOI(doiParts[0], doiParts[1]);
  }

  public static WebApplicationException throwBadRequest(String message) {
    throw new WebApplicationException(buildWebException(Response.Status.BAD_REQUEST, message));
  }

  public static WebApplicationException buildWebException(Response.Status status, String message) {
    return new WebApplicationException(Response.status(status).entity(message).build());
  }
}
