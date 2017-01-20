package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.datacite.DataCiteService;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static  org.gbif.datarepo.resource.validation.ResourceValidations.buildWebException;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateDoi;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateFiles;
import static  org.gbif.datarepo.resource.validation.ResourceValidations.validateMetadata;

/**
 *
 */
@Path("/data_packages")
@Produces(MediaType.APPLICATION_JSON)
public class DataRepoResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataRepoResource.class);

  // low quality of source to default to JSON
  private static final String OCT_STREAM_QS = ";qs=0.5";


  private final DataRepository dataRepository;
  private final DataCiteService dataCiteService;
  private final DataPackagesDoiGenerator doiGenerator;


  public DataRepoResource(DataRepository dataRepository, DataCiteService dataCiteService,
                          DataPackagesDoiGenerator doiGenerator) {
    this.dataRepository = dataRepository;
    this.dataCiteService = dataCiteService;
    this.doiGenerator = doiGenerator;
  }

  @POST
  @Timed
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public DataPackage uploadFile(FormDataMultiPart multiPart) throws IOException {
    DataPackage dataPackage = new DataPackage();
    List<FormDataBodyPart> files = validateFiles(multiPart);
    InputStream metadata  = validateMetadata(multiPart);
    DOI doi = doiGenerator.newDOI();
    dataPackage.setDoi(doi);
    files.stream().forEach( formDataBodyPart -> storeFile(dataPackage, formDataBodyPart));
    dataRepository.storeMetadata(doi, metadata);
    try {
      dataCiteService.register(doi, URI.create(dataPackage.getTargetUrl()),
                               CharStreams.toString(new InputStreamReader(metadata, Charsets.UTF_8)));
    } catch (DoiException ex) {
      LOGGER.error("Error registering a DOI", ex);
      dataRepository.delete(doi);
      throw buildWebException(Status.INTERNAL_SERVER_ERROR, "Error registering DOI");
    }
    return dataPackage;
  }


  @POST
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{doi}")
  public DataPackage get(@PathParam("doi") String doiRef)  {
    DOI doi = validateDoi(doiRef);
    java.nio.file.Path dataPackagePath = dataRepository.get(doi)
                                            .orElseThrow(() ->
                                                              buildWebException(Status.NOT_FOUND,
                                                                                String.format("DOI %s not found in repository", doiRef))
                                                        );
    DataPackage dataPackage = new DataPackage();
    dataPackage.setDoi(doi);
    Arrays.stream(
      dataPackagePath.toFile().listFiles(pathname -> !pathname.getName().equals("metadata.xml"))
    ).forEach(file -> dataPackage.addFile(file.getName()));
    return dataPackage;
  }


  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM + OCT_STREAM_QS)
  @Path("{doi}/file/{fileName}")
  public Response getFile(@PathParam("doi") String doiRef, @PathParam("fileName") String fileName,
                          @Context HttpServletResponse response) throws Exception {
    DOI doi = validateDoi(doiRef);
    Optional<InputStream> fileInputStream = dataRepository.getFile(doi, fileName);
    response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    return fileInputStream.isPresent()? Response.ok(fileInputStream.get())
                                                  .header("Content-Disposition",
                                                          "attachment; filename=" + fileName).build() :
                                        Response.status(Status.NOT_FOUND)
                                                  .entity(String.format("File %s not found",fileName)).build();
  }




  private void storeFile(DataPackage dataPackage, FormDataBodyPart formDataBodyPart) {
    dataRepository.store(dataPackage.getDoi(), formDataBodyPart.getValueAs(InputStream.class),
                         formDataBodyPart.getFormDataContentDisposition().getFileName());

  }



}
