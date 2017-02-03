package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.conf.DataCiteConfiguration;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.conf.DbConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.discovery.conf.ServiceConfiguration;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.ServiceConfig;
import org.gbif.doi.service.datacite.DataCiteService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Matchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;

import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.resource.ResourceTestUtils.TEST_DATA_PACKAGE_DIR;
import static org.gbif.datarepo.resource.PathsParams.METADATA_PARAM;
import static org.gbif.datarepo.resource.PathsParams.FILE_PARAM;
import static org.gbif.datarepo.resource.ResourceTestUtils.TEST_REPO_PATH;
import static org.gbif.datarepo.resource.ResourceTestUtils.CONTENT_TEST_FILE;
import static org.gbif.datarepo.resource.ResourceTestUtils.METADATA_TEST_FILE;
import static org.gbif.datarepo.resource.ResourceTestUtils.TEST_USER;
import static org.gbif.datarepo.resource.ResourceTestUtils.TEST_USER_CREDENTIALS;
import static org.gbif.datarepo.resource.ResourceTestUtils.TEST_BASIC_CREDENTIALS;
import static org.gbif.datarepo.resource.ResourceTestUtils.dataBodyPartOf;

import static org.gbif.datarepo.model.DataPackageDeSerTest.testDataPackage;
import static org.gbif.datarepo.model.DataPackageDeSerTest.TEST_DOI_SUFFIX;

/**
 * Test class for the DataPackageResource class.
 * Implements test cases for: data packages creation, retrieval and deletion.
 */
public class DataPackageResourceTest {

  private static final DataCiteService DATA_CITE_SERVICE = mock(DataCiteService.class);

  private static final GbifAuthenticator AUTHENTICATOR = mock(GbifAuthenticator.class);

  private static final DataPackage TEST_DATA_PACKAGE = testDataPackage();

  private static DataPackagesDoiGenerator doiGenerator;

  private static Path temporaryFolder;

  private static DataRepoConfiguration configuration;

  /**
   * Creates an temporary folder, the return instance is lazy evaluated into the field temporaryFolder.
   */
  private static Path temporaryFolder() {
    try {
      if (temporaryFolder == null) {
        //Creates the temporary folder using a random prefix
        temporaryFolder = Files.createTempDirectory(RandomStringUtils.random(3));
      }
      return temporaryFolder;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Returns or creates a new instance of a DataPackagesDoiGenerator class using test parameters and mock services.
   */
  private static DataPackagesDoiGenerator getDoiGenerator() {
    if (doiGenerator == null) {
      doiGenerator = new DataPackagesDoiGenerator("10.5072","dp.", DATA_CITE_SERVICE);
    }
    return doiGenerator;
  }

  /**
   * Returns or creates a new configuration object instance.
   * The temporaryFolder field is used as the repository directory.
   */
  private static DataRepoConfiguration configuration() {
    if (configuration == null) {
      configuration = new DataRepoConfiguration();
      configuration.setDoiShoulder("dp.");
      configuration.setDoiCommonPrefix("10.5072");
      configuration.setGbifApiUrl("http://localhost:8080/data_packages/");
      configuration.setGbifPortalUrl("http://www.gbif-dev.org/datapackage/");
      //Used the temporary folder as the data repo path
      configuration.setDataRepoPath(temporaryFolder().toString());
      DataCiteConfiguration dataCiteConfiguration = new DataCiteConfiguration();
      dataCiteConfiguration.setApiUrl("https://mds.datacite.org/");
      dataCiteConfiguration.setPassword("blah");
      dataCiteConfiguration.setUserName("DK.GBIF");
      configuration.setDataCiteConfiguration(dataCiteConfiguration);
      configuration.setUsersDb(mock(DbConfiguration.class));
      //empty ServiceConfiguration instance to avoid make it discoverable in Zookeeper
      configuration.setService(new ServiceConfiguration());
    }
    return configuration;
  }

  //Grizzly is required since he in-memory Jersey test container does not support all features,
  // such as the @Context injection used by BasicAuthFactory and OAuthFactory.
  @ClassRule
  public static ResourceTestRule resource = ResourceTestRule.builder()
    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
    //required to send multiple files
    .addProvider(MultiPartFeature.class)
    //Authentication
    .addProvider(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<UserPrincipal>()
                                          .setAuthenticator(AUTHENTICATOR)
                                          .setRealm(GbifAuthenticator.GBIF_REALM).buildAuthFilter()))
    .addProvider(new AuthValueFactoryProvider.Binder<>(UserPrincipal.class))
    //Test resource
    .addResource(new DataPackageResource(new FileSystemRepository(configuration(), getDoiGenerator(),
                                                                  DATA_CITE_SERVICE), configuration()))
    .build();

  /**
   * Initialized all the elements used across all test cases.
   */
  @BeforeClass
  public static void init() {
    temporaryFolder();
    getDoiGenerator();
    configuration();
  }

  /**
   * Removes the temporary folder created for testing.
   */
  @AfterClass
  public static void destroy() throws IOException {
    if (temporaryFolder != null && temporaryFolder.toFile().exists()) {
      FileUtils.deleteDirectory(temporaryFolder.toFile());
    }
  }

  /**
   * Executed before each test case. Initializes all mock objects.
   */
  @Before
  public void setup() {
    try {
      //Copies all the content from  'testrepo/10.5072-dp.bvmv02' to the temporary directory
      //This is done to test service to retrieve DataPackages information and files
      FileUtils.copyDirectory(new File(TEST_REPO_PATH), temporaryFolder.toFile());
      //Mocks DOI existence check by always returning True
      when(DATA_CITE_SERVICE.exists(any(DOI.class))).thenReturn(false);
      //Do nothing for all registration calls
      doNothing().when(DATA_CITE_SERVICE).register(any(DOI.class), any(URI.class), any(DataCiteMetadata.class));
      //Always return True when deleting DOIs
      when(DATA_CITE_SERVICE.delete(any(DOI.class))).thenReturn(true);
      //Only the TEST_USER is valid
      when(AUTHENTICATOR.authenticate(Matchers.eq(TEST_BASIC_CREDENTIALS))).thenReturn(Optional.of(TEST_USER));
    } catch (AuthenticationException | DoiException | IOException  ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Executed after each test, resets all the mock instances.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(DATA_CITE_SERVICE);
    reset(AUTHENTICATOR);
  }

  /**
   * Tests that the content of directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testGetDataPackage() throws DoiException {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, TEST_DOI_SUFFIX).toString())
                 .request().get(DataPackage.class))
      .isEqualTo(TEST_DATA_PACKAGE);
  }

  /**
   * Tests that the test users can upload files and create a new DataPackage instance.
   */
  @Test
  public void testCreateDataPackage() throws Exception {
    try (MultiPart multiPart = new FormDataMultiPart().bodyPart(dataBodyPartOf(CONTENT_TEST_FILE, FILE_PARAM))
                                                      .bodyPart(dataBodyPartOf(METADATA_TEST_FILE, METADATA_PARAM))) {
      DataPackage newDataPackage = resource.getJerseyTest()
                   .target(DATA_PACKAGES_PATH)
                   .register(MultiPartFeature.class)
                   .request()
                   .header(HttpHeaders.AUTHORIZATION, TEST_USER_CREDENTIALS)
                   .post(Entity.entity(multiPart, multiPart.getMediaType()), DataPackage.class);
      //Test that both packages contains the same elements
      assertThat(newDataPackage).isEqualTo(buildTestPackage(newDataPackage.getDoi()));
      //verify that exists method was invoked
      verify(DATA_CITE_SERVICE).exists(any(DOI.class));
      verify(DATA_CITE_SERVICE).register(any(DOI.class), any(URI.class), any(DataCiteMetadata.class));
    }
  }

  /**
   * Test that the file 'testrepo/10.5072-dp.bvmv02/occurrence.txt' can be retrieved.
   */
  @Test
  public void testGetFile() throws IOException {
    try (InputStream downloadFile = resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH,
                                                                              TEST_DOI_SUFFIX,
                                                                              CONTENT_TEST_FILE).toString())
                                                            .request().get(InputStream.class); //download file
         //Read test file
         InputStream contentFile = new FileInputStream(Paths.get(TEST_DATA_PACKAGE_DIR, CONTENT_TEST_FILE).toFile())) {
      //compare file contents
      assertThat(downloadFile).hasSameContentAs(contentFile);
    }
  }


  /**
   * Tests that the content of directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testDeleteDataPackage() throws DoiException {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, TEST_DOI_SUFFIX).toString())
                 .request().delete().getStatus())
      .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    verify(DATA_CITE_SERVICE).delete(any(DOI.class));
  }

  /**
   * Builds a test instance using configured test resources.
   */
  public static DataPackage buildTestPackage(URI doi) {
    DataPackage dataPackage = new DataPackage(configuration.getGbifApiUrl()
                                              + Paths.get(doi.getPath()).getFileName() + '/');
    dataPackage.setMetadata(METADATA_TEST_FILE);
    dataPackage.addFile(CONTENT_TEST_FILE);
    dataPackage.setDoi(doi);
    return dataPackage;
  }
}
