package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.datarepo.api.FileInputContent;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static  org.gbif.datarepo.resource.validation.ResourceValidations.buildWebException;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateDoi;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFiles;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFileSubmitted;
import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.resource.PathsParams.METADATA_PARAM;

/**
 * Data packages resource.
 * Exposes the RESTful interface to create, update metadata, retrieve and delete GBIF data packages.
 */
@Path(DATA_PACKAGES_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class DataPackageResource {

  private static final Logger LOG = LoggerFactory.getLogger(DataPackageResource.class);

  // low quality of source to default to JSON
  private static final String OCT_STREAM_QS = ";qs=0.5";
  private static final String FILE_ATTACHMENT = "attachment; filename=";


  private final DataRepository dataRepository;
  private final DataRepoConfiguration configuration;

  /**
   * Full constructor.
   */
  public DataPackageResource(DataRepository dataRepository, DataRepoConfiguration configuration) {
    this.dataRepository = dataRepository;
    this.configuration = configuration;
  }

  /**
   * Creates a new data package. The parameters: file(multiple values), metadata are required. Only authenticated
   * user are allowed to create data packages. A new DOI is created and assigned as a Identifier.
   *
   */
  @POST
  @Timed
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public DataPackage create(FormDataMultiPart multiPart, @Auth UserPrincipal principal) throws IOException {
    //Validations
    validateFileSubmitted(multiPart, METADATA_PARAM);
    List<FormDataBodyPart> files = validateFiles(multiPart);
    try {
      return  dataRepository.create(principal.getName(), //user
                                    multiPart.getField(METADATA_PARAM).getValueAs(InputStream.class), //metadata file
                                    //files
                                    files.stream().map(bodyPart ->
                                                         FileInputContent.of(bodyPart.getFormDataContentDisposition()
                                                                               .getFileName(),
                                                                             bodyPart.getValueAs(InputStream.class)))
                                    .collect(Collectors.toList()));
    } catch (Exception ex) {
      LOG.error("Error creating data package", ex);
      throw buildWebException(Status.INTERNAL_SERVER_ERROR, "Error registering DOI");
    }
  }

  /**
   * Retrieves a DataPackage by its DOI suffix.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{doi}")
  public DataPackage get(@PathParam("doi") String doiSuffix)  {
    //Validates DOI structure
    DOI doi = validateDoi(configuration.getDoiCommonPrefix(), doiSuffix);

    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    return getOrNotFound(doi, doiSuffix);

  }

  /**
   * Retrieves a file contained in a data package.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_OCTET_STREAM + OCT_STREAM_QS)
  @Path("{doi}/{fileName}")
  public Response getFile(@PathParam("doi") String doiRef, @PathParam("fileName") String fileName) throws Exception {
    //Validation
    DOI doi = validateDoi(configuration.getDoiCommonPrefix(), doiRef);

    //Tries to get the file
    Optional<InputStream> fileInputStream = dataRepository.getFileInputStream(doi, fileName);

    //Check file existence before send it in the Response
    return fileInputStream.isPresent()? Response.ok(fileInputStream.get())
                                                    .header(HttpHeaders.CONTENT_DISPOSITION, FILE_ATTACHMENT + fileName)
                                                    .build() :
                                        Response.status(Status.NOT_FOUND)
                                          .entity(String.format("File %s not found", fileName)).build();
  }

  /**
   * Retrieves a DataPackage by its DOI suffix.
   */
  @DELETE
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{doi}")
  public void delete(@PathParam("doi") String doiSuffix)  {
    //Validates DOI structure
    DOI doi = validateDoi(configuration.getDoiCommonPrefix(), doiSuffix);

    //Checks that the DataPackage exists
    getOrNotFound(doi, doiSuffix);

    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    dataRepository.delete(doi);
  }


  /**
   * Gets a DataPackage form a DOI, throw HTTP NOT_FOUND exception if the elements is not found.
   */
  private DataPackage getOrNotFound(DOI doi, String doiSuffix) {
    return dataRepository.get(doi)
      .orElseThrow(() -> buildWebException(Status.NOT_FOUND,
                                           String.format("DOI %s not found in repository", doiSuffix)));
  }

  /**
   * Builds a API based URL of DOI assigned to a DataPackage.
   */
  private String dataPackageBaseUrl(DOI doi) {
    return configuration.getGbifApiUrl() + doi.getSuffix() + '/';
  }


}
