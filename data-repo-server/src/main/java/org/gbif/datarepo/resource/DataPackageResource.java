package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.doi.service.datacite.DataCiteValidator;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static  org.gbif.datarepo.resource.validation.ResourceValidations.buildWebException;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateDoi;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFiles;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFileSubmitted;

/**
 * Data packages resource.
 * Exposes the RESTful interface to create, update metadata, retrieve and delete GBIF data packages.
 */
@Path("/data_packages")
@Produces(MediaType.APPLICATION_JSON)
public class DataPackageResource {

  private static final String METADATA_PARAM = "metadata";
  private static final String METADATA_FILE = METADATA_PARAM + ".xml";

  private static final Logger LOG = LoggerFactory.getLogger(DataPackageResource.class);

  // low quality of source to default to JSON
  private static final String OCT_STREAM_QS = ";qs=0.5";


  private final DataRepository dataRepository;
  private final DataCiteService dataCiteService;
  private final DataPackagesDoiGenerator doiGenerator;
  private final DataRepoConfiguration configuration;

  /**
   * Full constructor.
   */
  public DataPackageResource(DataRepository dataRepository, DataCiteService dataCiteService,
                             DataPackagesDoiGenerator doiGenerator, DataRepoConfiguration configuration) {
    this.dataRepository = dataRepository;
    this.dataCiteService = dataCiteService;
    this.doiGenerator = doiGenerator;
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
  public DataPackage create(FormDataMultiPart multiPart, @Auth UserPrincipal userPrincipal) throws IOException {
    //Validations
    validateFileSubmitted(multiPart, METADATA_PARAM);
    List<FormDataBodyPart> files = validateFiles(multiPart);
    //Generate DOI
    DOI doi = doiGenerator.newDOI();
    //Store metadata.xml file
    dataRepository.storeMetadata(doi, multiPart.getField(METADATA_PARAM).getValueAs(InputStream.class));
    File metadataFile = dataRepository.get(doi).get().resolve(METADATA_FILE).toFile();
    //Generates a DataCiteMetadata object for further valdiation/manipulation
    DataCiteMetadata dataCiteMetadata = processMetadata(metadataFile, doi);
    try {
      //store all the submitted files
      files.stream().forEach( formDataBodyPart -> storeFile(doi, formDataBodyPart));
      //register the new DOI into DataCite
      dataCiteService.register(doi, targetDoiUrl(doi), dataCiteMetadata);
    } catch (DoiException ex) {
      LOG.error("Error registering a DOI", ex);
      //Deletes all data created to this DOI in case of error
      dataRepository.delete(doi);
      throw buildWebException(Status.INTERNAL_SERVER_ERROR, "Error registering DOI");
    }
    return get(doi.getSuffix());
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

    //Checks DOI existence
    Optional<java.nio.file.Path> dataPackagePath = dataRepository.get(doi);
    if (!dataPackagePath.isPresent()) {
      throw buildWebException(Status.NOT_FOUND, String.format("DOI %s not found in repository", doiSuffix));
    }

    //Assemble a new DataPackage instance containing all the information
    DataPackage dataPackage = new DataPackage(dataPackageBaseUrl(doi));
    dataPackage.setDoi(doi.getUrl());
    Arrays.stream(dataPackagePath.get().toFile().listFiles(pathname -> !pathname.getName().equals(METADATA_FILE)))
      .forEach(file -> dataPackage.addFile(file.getName())); //metadata.xml is excluded from the list of files
    return dataPackage;
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
    Optional<InputStream> fileInputStream = dataRepository.getFile(doi, fileName);

    //Check file existence before send it in the Response
    return fileInputStream.isPresent()? Response.ok(fileInputStream.get())
                                                  .header("Content-Disposition",
                                                          "attachment; filename=" + fileName).build() :
                                        Response.status(Status.NOT_FOUND)
                                                  .entity(String.format("File %s not found",fileName)).build();
  }

  /**
   * Stores a submitted a file into the data repository.
   */
  private void storeFile(DOI doi, FormDataBodyPart formDataBodyPart) {
    dataRepository.store(doi, formDataBodyPart.getValueAs(InputStream.class),
                         formDataBodyPart.getFormDataContentDisposition().getFileName());

  }

  /**
   * Builds a API based URL of DOI assigned to a DataPackage.
   */
  private String dataPackageBaseUrl(DOI doi) {
    return configuration.getGbifApiUrl() + doi.getSuffix() + "/";
  }

  /**
   * Builds a target Url for a DataPackage doi.
   */
  private URI targetDoiUrl(DOI doi) {
    return URI.create(configuration.getGbifPortalUrl() + doi.getDoiName());
  }

  /**
   * Reads, validates and stores the submitted metadata file.
   */
  private DataCiteMetadata processMetadata(File metadataFile, DOI doi) {
   try(InputStream inputStream = new FileInputStream(metadataFile)) {
     DataCiteMetadata dataCiteMetadata = DataCiteValidator.fromXml(inputStream);
     dataCiteMetadata.setIdentifier(DataCiteMetadata.Identifier.builder()
                                      .withValue(doi.getDoiName())
                                      .withIdentifierType(IdentifierType.DOI.name())
                                      .build());
     dataRepository.storeMetadata(doi, new ByteArrayInputStream(DataCiteValidator.toXml(doi, dataCiteMetadata)
                                                                  .getBytes(StandardCharsets.UTF_8)));
     return dataCiteMetadata;
   } catch (JAXBException | IOException | InvalidMetadataException ex) {
     LOG.error("Error reading data package {} metadata", doi, ex);
     dataRepository.delete(doi);
     throw buildWebException(Status.BAD_REQUEST, "Error reading metadata");
   }
 }
}
