package org.gbif.datarepo.resource.validation;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;


/**
 * Utility class to process common validation across Data packages resources/web services.
 */
public class ResourceValidations {

  /**
   * Private constructor.
   */
  private  ResourceValidations() {
    //empty constructor
  }

  /**
   * Validate that the input list from file denoted by the Http form param 'file' is not empty.
   */
  public static List<FormDataBodyPart> validateFiles(List<FormDataBodyPart> files, List<String> urlFiles) {
    if ((files == null || files.isEmpty()) && (urlFiles == null || urlFiles.isEmpty())) {
      //if list if files is empty throw a BadRequest response.
      throw buildWebException(Response.Status.BAD_REQUEST, "Data package must contain at least 1 file");
    }
    return files;
  }

  /**
   * Throws a WebApplicationException containing a BadRequest status.
   */
  public static void throwBadRequest(String message) {
    throw buildWebException(Response.Status.BAD_REQUEST, message);
  }

  /**
   * Returns a new WebApplicationException with a status code and an error message.
   */
  public static WebApplicationException buildWebException(Response.Status status, String message) {
    return new WebApplicationException(Response.status(status).entity(message).build());
  }

  /**
   * Returns a new WebApplicationException with a status code and an error message.
   */
  public static WebApplicationException buildWebException(Throwable throwable, Response.Status status, String message) {
    if (throwable instanceof WebApplicationException) {
      return (WebApplicationException)throwable;
    }
    return new WebApplicationException(throwable, Response.status(status).entity(message).build());
  }

}
