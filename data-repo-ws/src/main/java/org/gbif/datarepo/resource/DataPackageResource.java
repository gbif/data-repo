package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.app.DataRepoConfigurationDW;
import org.gbif.datarepo.citation.CitationGenerator;
import org.gbif.datarepo.identifiers.orcid.OrcidPublicService;
import org.gbif.datarepo.registry.JacksonObjectMapperProvider;
import org.gbif.datarepo.impl.download.FileDownload;
import org.gbif.datarepo.impl.conf.DataRepoConfiguration;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gbif.datarepo.resource.PathsParams.FILE_PARAM;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.buildWebException;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFiles;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.throwBadRequest;
import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.resource.PathsParams.RELATED_IDENTIFIERS_PATH;
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

  private static final String DATA_REPO_ACCESS_ROLE ="DATA_REPO_USER";

  private final DataRepository dataRepository;

  private final DataPackageUriBuilder uriBuilder;

  private final FileDownload downloadHandler;

  private final IdentifiersValidator identifiersValidator;

  private final Validator validator;

  private final OrcidPublicService orcidPublicService;

  /**
   * Full constructor.
   */
  public DataPackageResource(DataRepository dataRepository, DataRepoConfigurationDW configuration, Validator validator,
                             OrcidPublicService orcidPublicService) {
    this.dataRepository = dataRepository;
    DataRepoConfiguration dataRepoConfiguration = configuration.getDataRepoConfiguration();
    uriBuilder = new DataPackageUriBuilder(dataRepoConfiguration.getDataPackageApiUrl());
    downloadHandler = new FileDownload(dataRepoConfiguration.getFileSystem());
    identifiersValidator = new IdentifiersValidator(dataRepository, downloadHandler);
    this.validator = validator;
    this.orcidPublicService = orcidPublicService;
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
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{dataPackageIdentifier}/" + RELATED_IDENTIFIERS_PATH)
  public PagingResponse<Identifier> listIdentifiers(@Nullable @QueryParam("user") String user,
                                                    @Nullable @BeanParam PagingParam page,
                                                    @Nullable @QueryParam("identifier") String identifier,
                                                    @Nullable @PathParam("dataPackageIdentifier") String dataPackageIdentifier,
                                                    @Nullable @QueryParam("type") Identifier.Type type,
                                                    @Nullable @QueryParam("relationType") Identifier.RelationType relationType,
                                                    @Nullable @QueryParam("created") Date created) {
    UUID dataPackageKey = getDataPackageByIdentifier(dataPackageIdentifier).map(DataPackage::getKey).orElse(null);
    return dataRepository.listIdentifiers(user, page, identifier, dataPackageKey, type, relationType, created);
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
      //Validates all javax.validation annotations
      validateDataPackage(dataPackage);
      dataPackage.setRelatedIdentifiers(identifiersValidator.validateIdentifiers(multiPart, dataPackage.getRelatedIdentifiers()));
      dataPackage.setCreatedBy(principal.getName());
      DataPackage newDataPackage = dataRepository.create(dataPackage, streamFiles(files, urlFiles), true);
      return newDataPackage.inUrl(uriBuilder.build(newDataPackage.getKey()));
    } catch (Exception ex) {
      LOG.error("Error creating data package", ex);
      throw buildWebException(ex, Status.INTERNAL_SERVER_ERROR, "Error creating data package");
    }
  }

  /**
   * Performs all bean validations defined in the class DataPackage.
   */
  private void validateDataPackage(DataPackage dataPackage) {
    try {
      Set<ConstraintViolation<DataPackage>> violations = validator.validate(dataPackage);
      if (!violations.isEmpty()) {
        throwBadRequest("Invalid DataPackage definition: "
                        + violations.stream()
                          .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                          .collect(Collectors.joining(System.lineSeparator())));
      }
    } catch (ValidationException ex) {
      throwBadRequest(ex.getMessage());
    }
    validateOrcidsExist(dataPackage);
  }

  private void validateOrcidsExist(DataPackage dataPackage) {
    if (dataPackage.getCreators() != null) {
      dataPackage.getCreators().stream()
        .filter(creator -> Creator.IdentifierScheme.ORCID == creator.getIdentifierScheme()
                           && creator.getIdentifier() != null)
        .collect(Collectors.toSet()).forEach(orcider -> {
        if (!orcidPublicService.exists(orcider.getIdentifier())) {
          throwBadRequest("The orcid " + orcider.getIdentifier() + " does not exist");
        }
      });
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
  @Path("{identifier}")
  public DataPackage get(@PathParam("identifier") String identifier)  {
    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    return getOrNotFound(identifier);
  }


  /**
   * Retrieves a DataPackage by its DOI suffix.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_XML)
  @Path("{identifier}/metadata")
  public Response getMetadata(@PathParam("identifier") String identifier)  {
    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    DataPackage dataPackage = getOrNotFound(identifier);
    //Tries to get the file
    Optional<InputStream> fileInputStream = dataRepository.getFileInputStream(dataPackage.getKey(),
                                                                              dataPackage.getKey() + ".xml");

        //Check file existence before send it in the Response
    return fileInputStream.isPresent() ? Response.ok(fileInputStream.get()).build()
      : Response.status(Status.NOT_FOUND)
        .entity(String.format("Metadata file not found")).build();

  }


  /**
   * Retrieves a file contained in a data package.
   */
  @GET
  @Timed
  @Produces(MediaType.APPLICATION_OCTET_STREAM + OCT_STREAM_QS)
  @Path("{identifier}/{fileName}")
  public Response getFile(@PathParam("identifier") String identifier, @PathParam("fileName") String fileName)  {
    DataPackage dataPackage = getOrNotFound(identifier);
    //Tries to get the file
    Optional<InputStream> fileInputStream = dataRepository.getFileInputStream(dataPackage.getKey(), fileName);

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
  @Path("{identifier}/{fileName}/data")
  public Response getFileData(@PathParam("identifier") String identifier, @PathParam("fileName") String fileName)  {
    DataPackage dataPackage = getOrNotFound(identifier);

    //Tries to get the file
    Optional<DataPackageFile> dataPackageFile = dataRepository.getFile(dataPackage.getKey(), fileName);

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
  @Path("{identifier}")
  @RolesAllowed(DATA_REPO_ACCESS_ROLE)
  public void delete(@PathParam("identifier") String identifier, @Auth GbifUserPrincipal principal)  {

    //Checks that the DataPackage exists
    DataPackage dataPackage = getOrNotFound(identifier);
    if (!dataPackage.getCreatedBy().equals(principal.getUser().getUserName())) {
      throw buildWebException(Status.UNAUTHORIZED, "A Data Package can be deleted only by its creator");
    }

    //Gets the data package, throws a NOT_FOUND error if it doesn't exist
    dataRepository.delete(dataPackage.getKey());
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("vocabulary/licenseType")
  /**
   * Return the list of supported identifier types.
   */
  public Collection<License> getLicenseTypes() {
    return EnumSet.complementOf(EnumSet.of(License.UNSPECIFIED, License.UNSUPPORTED));
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("vocabulary/identifierType")
  /**
   * Return the list of supported identifier types.
   */
  public Identifier.Type[] getIdentifierTypes() {
    return Identifier.Type.values();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("vocabulary/identifierRelationType")
  /**
   * Return the list of supported identifier relation types.
   */
  public Identifier.RelationType[] getIdentifierRelationTypes() {
    return Identifier.RelationType.values();
  }

  @POST
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("citationPreview")
  public String citationPreview(DataPackage dataPackage) {
    return CitationGenerator.generateCitation(dataPackage);
  }

  /**
   * Gets a DataPackage form a DOI, throw HTTP NOT_FOUND exception if the elements is not found.
   */
  private DataPackage getOrNotFound(String identifier) {
    return getDataPackageByIdentifier(identifier).orElseThrow(() -> buildWebException(Status.NOT_FOUND,
                                           String.format("Identifier %s not found in repository", identifier)))
      .inUrl(uriBuilder.build(identifier));
  }

  /**
   * Tries to get a DataPackage by UUID, DOI or an alternative identifier.
   */
  private Optional<DataPackage> getDataPackageByIdentifier(String identifier) {
    Optional<DataPackage> dataPackage = getByUUID(identifier);
    if (!dataPackage.isPresent()) {
      dataPackage = getByDOI(identifier);
      if (!dataPackage.isPresent()) {
        dataPackage = getByAlternativeIdentifier(identifier);
      }
    }
    return  dataPackage;
  }

  /**
   * Get a DataPackage by UUID.
   */
  private Optional<DataPackage> getByUUID(String identifier) {
   try {
     UUID dataPackageKey = UUID.fromString(identifier);
     return dataRepository.get(dataPackageKey);
   } catch (Exception ex) {
     return Optional.empty();
   }
  }

  /**
   * Get a DataPackage by DOI.
   */
  private Optional<DataPackage> getByDOI(String identifier) {
    try {
      DOI doi = new DOI(identifier);
      return dataRepository.get(doi);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Get a DataPackage by Alternative identifier.
   */
  private Optional<DataPackage> getByAlternativeIdentifier(String identifier) {
      return dataRepository.getByAlternativeIdentifier(identifier);
  }

}
