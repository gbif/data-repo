package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.app.DataRepoConfigurationDW;
import org.gbif.datarepo.registry.JacksonObjectMapperProvider;
import org.gbif.datarepo.store.fs.download.FileDownload;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gbif.datarepo.resource.PathsParams.FILE_PARAM;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.buildWebException;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateDoi;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFiles;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFileSubmitted;
import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.resource.PathsParams.METADATA_PARAM;
import static org.gbif.datarepo.resource.PathsParams.DP_FORM_PARAM;
import static org.gbif.datarepo.resource.PathsParams.FILE_URL_PARAM;

/**
 * Data packages resource.
 * Exposes the RESTful interface to create, update metadata, retrieve and delete GBIF data packages.
 */
@Path(DATA_PACKAGES_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class DataPackageResource {

  private static final Logger LOG = LoggerFactory.getLogger(DataPackageResource.class);

  // low quality from source to default to JSON
  private static final String OCT_STREAM_QS = ";qs=0.5";

  private static final String FILE_ATTACHMENT = "attachment; filename=";

  private static final String DATA_REPO_ACCESS_ROLE = "REGISTRY_ADMIN";

  private final DataRepository dataRepository;

  private final DataPackageUriBuilder uriBuilder;

  private final FileDownload downloadHandler;

  /**
   * Full constructor.
   */
  public DataPackageResource(DataRepository dataRepository, DataRepoConfigurationDW configuration) {
    this.dataRepository = dataRepository;
    DataRepoConfiguration dataRepoConfiguration = configuration.getDataRepoConfiguration();
    uriBuilder = new DataPackageUriBuilder(dataRepoConfiguration.getDataPackageApiUrl());
    downloadHandler = new FileDownload(dataRepoConfiguration.getFileSystem());
  }

  /**
   * Creates a new data package. The parameters: file(multiple values), metadata are required. Only authenticated
   * user are allowed to create data packages. A new DOI is created and assigned as a Identifier.
   *
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public PagingResponse<DataPackage> list(@Nullable @QueryParam("user") String user,
                                          @Nullable @BeanParam PagingParam page,
                                          @Nullable @QueryParam("fromDate") Date fromDate,
                                          @Nullable @QueryParam("toDate") Date toDate,
                                          @Nullable @QueryParam("tag") List<String> tags,
                                          @Nullable @QueryParam("q") String q) {
    return dataRepository.list(user, page, fromDate, toDate, false, tags, q);
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
  @RolesAllowed(DATA_REPO_ACCESS_ROLE)
  public DataPackage create(FormDataMultiPart multiPart, @Auth GbifUserPrincipal principal,
                            @Context HttpServletRequest request) throws IOException {
    //Validations
    validateFileSubmitted(multiPart, METADATA_PARAM);
    List<FormDataBodyPart> files = multiPart.getFields(FILE_PARAM);

    List<String> urlFiles = Optional.ofNullable(multiPart.getFields(FILE_URL_PARAM))
                              .map(formDataBodyParts -> formDataBodyParts.stream().map(FormDataBodyPart::getValue)
                                .collect(Collectors.toList()))
                              .orElse(Collections.emptyList());
    //check that files + urlFiles are not empty
    validateFiles(files, urlFiles);
    checkFileLocations(urlFiles);
    try {
      DataPackage dataPackage = JacksonObjectMapperProvider.MAPPER
                                  .readValue(multiPart.getField(DP_FORM_PARAM).getValueAs(String.class),
                                             DataPackage.class);
      dataPackage.setCreatedBy(principal.getName());
      DataPackage newDataPackage = dataRepository.create(dataPackage,
                                                         multiPart.getField(METADATA_PARAM)
                                                           .getValueAs(InputStream.class),
                                                         streamFiles(files, urlFiles));
      return newDataPackage.inUrl(uriBuilder.build(newDataPackage.getDoi()));
    } catch (Exception ex) {
      LOG.error("Error creating data package", ex);
      throw buildWebException(ex, Status.INTERNAL_SERVER_ERROR, "Error registering DOI");
    }
  }

  /**
   * Validates that the specified file locations are reachable form this service.
   */
  private void checkFileLocations(List<String> fileLocations) {
    Optional.ofNullable(fileLocations).ifPresent(
      locations -> locations.forEach(fileUri -> {
        try {
          if (!downloadHandler.exists(fileUri)) {
            throw new BadRequestException("File location is not reachable " + fileUri);
          }
        } catch (IOException ex){
          LOG.error("Error checking file existence", ex);
          throw buildWebException(ex, Status.INTERNAL_SERVER_ERROR, "Error reading file " + fileUri);
        }
      })
    );
  }

  /**
   * Combines submitted files and urls in single list of input content.
   */
  private static List<FileInputContent> streamFiles(List<FormDataBodyPart> files, List<String> urlFiles) {
    List<FileInputContent> fileInputContents = new ArrayList<>();
    Optional.ofNullable(files)
      .ifPresent(streamFiles -> streamFiles
                                  .forEach(bodyPart -> fileInputContents.add(FileInputContent
                                                                               .from(bodyPart.getFormDataContentDisposition().getFileName(),
                                                                                     bodyPart.getValueAs(InputStream.class)))
    ));
    Optional.ofNullable(urlFiles)
      .ifPresent(streamUrlFiles -> streamUrlFiles.forEach(urlFile -> {
        try {
          URI uri = new URI(urlFile);
          fileInputContents.add(FileInputContent.from(Paths.get(uri.getPath()).getFileName().toString(), uri));
        } catch (URISyntaxException ex) {
          String message = String.format("Wrong URI %s", urlFile);
          LOG.error(message, ex);
          throw new BadRequestException(message) ;
        }
      }));
    return fileInputContents;
  }

  /**
   * Retrieves a DataPackage by its DOI suffix.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{doi}")
  public DataPackage get(@PathParam("doi") DOI doi)  {
    //Validates DOI structure
    validateDoi(doi.getPrefix(), doi.getSuffix());

    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    return getOrNotFound(doi, doi.getDoiName());

  }

  /**
   * Retrieves a file contained in a data package.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_OCTET_STREAM + OCT_STREAM_QS)
  @Path("{doi}/{fileName}")
  public Response getFile(@PathParam("doi") DOI doi, @PathParam("fileName") String fileName)  {
    //Validation
    validateDoi(doi.getPrefix(), doi.getSuffix());

    //Tries to get the file
    Optional<InputStream> fileInputStream = dataRepository.getFileInputStream(doi, fileName);

    //Check file existence before send it in the Response
    return fileInputStream.isPresent() ? Response.ok(fileInputStream.get())
                                                    .header(HttpHeaders.CONTENT_DISPOSITION, FILE_ATTACHMENT + fileName)
                                                    .build()
                                        : Response.status(Status.NOT_FOUND)
                                          .entity(String.format("File %s not found", fileName)).build();
  }

  /**
   * Retrieves a file contained in a data package.
   */
  @GET
  @Timed
  @Path("{doi}/{fileName}/data")
  public Response getFileData(@PathParam("doi") DOI doi, @PathParam("fileName") String fileName)  {
    //Validation
    validateDoi(doi.getPrefix(), doi.getSuffix());

    //Tries to get the file
    Optional<DataPackageFile> dataPackageFile = dataRepository.getFile(doi, fileName);

    //Check file existence before send it in the Response
    return dataPackageFile.isPresent() ? Response.ok(dataPackageFile.get()).build()
      : Response.status(Status.NOT_FOUND).entity(String.format("File %s not found", fileName)).build();
  }

  /**
   * Retrieves a DataPackage by its DOI suffix.
   */
  @DELETE
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{doi}")
  @RolesAllowed(DATA_REPO_ACCESS_ROLE)
  public void delete(@PathParam("doi") DOI doi, @Auth GbifUserPrincipal principal)  {
    //Validates DOI structure
    validateDoi(doi.getPrefix(), doi.getSuffix());

    //Checks that the DataPackage exists
    DataPackage dataPackage = getOrNotFound(doi, doi.getDoiName());
    if (!dataPackage.getCreatedBy().equals(principal.getUser().getUserName())) {
      throw buildWebException(Status.UNAUTHORIZED, "A Data Package can be deleted only by its creator");
    }

    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    dataRepository.delete(doi);
  }

  /**
   * Gets a DataPackage form a DOI, throw HTTP NOT_FOUND exception if the elements is not found.
   */
  private DataPackage getOrNotFound(DOI doi, String doiSuffix) {
    return dataRepository.get(doi)
      .orElseThrow(() -> buildWebException(Status.NOT_FOUND,
                                           String.format("DOI %s not found in repository", doiSuffix)))
      .inUrl(uriBuilder.build(doi));
  }



}
