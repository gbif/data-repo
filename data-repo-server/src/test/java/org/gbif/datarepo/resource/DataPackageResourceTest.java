package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.auth.GbifAuthenticator;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.conf.DbConfiguration;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.test.mocks.DataPackageMapperMock;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.test.mocks.DoiRegistrationServiceMock;
import org.gbif.discovery.conf.ServiceConfiguration;
import org.gbif.doi.service.DoiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
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
import org.assertj.core.api.Condition;
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
import static org.mockito.Mockito.spy;

import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_DATA_PACKAGE_DIR;
import static org.gbif.datarepo.resource.PathsParams.METADATA_PARAM;
import static org.gbif.datarepo.resource.PathsParams.FILE_PARAM;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_REPO_PATH;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.CONTENT_TEST_FILE;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.METADATA_TEST_FILE;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_USER;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_USER_CREDENTIALS;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_BASIC_CREDENTIALS;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.dataBodyPartOf;

import static org.gbif.datarepo.api.model.DataPackageDeSerTest.testDataPackage;
import static org.gbif.datarepo.api.model.DataPackageDeSerTest.TEST_DOI_SUFFIX;

/**
 * Test class for the DataPackageResource class.
 * Implements test cases for: data packages creation, retrieval and deletion.
 */
public class DataPackageResourceTest {

  private static final GenericType<PagingResponse<DataPackage>> GENERIC_PAGING_RESPONSE = new GenericType<PagingResponse<DataPackage>>(){};
  private static final GbifAuthenticator AUTHENTICATOR = mock(GbifAuthenticator.class);

  private static final DataPackageMapper DATA_PACKAGE_MAPPER = spy(new DataPackageMapperMock(configuration()));

  private static final DataPackage TEST_DATA_PACKAGE = testDataPackage();

  private static Path temporaryFolder;

  private static DataRepoConfiguration configuration;

  private DataPackageUriBuilder uriBuilder;

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
   * Returns or creates a new configuration object instance.
   * The temporaryFolder field is used as the repository directory.
   */
  private static DataRepoConfiguration configuration() {
    if (configuration == null) {
      configuration = new DataRepoConfiguration();
      configuration.setDoiCommonPrefix("10.5072");
      configuration.setGbifApiUrl("http://localhost:8080/");
      //Used the temporary folder as the data repo path
      configuration.setDataRepoPath(temporaryFolder().toString());
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
    .addResource(new DataPackageResource(new FileSystemRepository(configuration(), new DoiRegistrationServiceMock(),
                                                                  DATA_PACKAGE_MAPPER),
                                         configuration()))
    .build();

  /**
   * Initialized all the elements used across all test cases.
   */
  @BeforeClass
  public static void init() {
    temporaryFolder();
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
      uriBuilder = new DataPackageUriBuilder(configuration.getDataPackageApiUrl());
      //Copies all the content from  'testrepo/10.5072-dp.bvmv02' to the temporary directory
      //This is done to test service to retrieve DataPackages information and files
      FileUtils.copyDirectory(new File(TEST_REPO_PATH), temporaryFolder.toFile());
      //Only the TEST_USER is valid
      when(AUTHENTICATOR.authenticate(Matchers.eq(TEST_BASIC_CREDENTIALS))).thenReturn(Optional.of(TEST_USER));
    } catch (AuthenticationException | IOException  ex) {
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
    reset(AUTHENTICATOR);
  }

  /**
   * Tests that the content of directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testGetDataPackage() {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, TEST_DOI_SUFFIX).toString())
                 .request().get(DataPackage.class))
      .isEqualTo(TEST_DATA_PACKAGE);
  }


  /**
   * Tests that the content of directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testListDataPackage()  {
    assertThat(resource.getJerseyTest().target(DATA_PACKAGES_PATH).request().get(GENERIC_PAGING_RESPONSE))
      .has(new Condition<PagingResponse<DataPackage>>() {
        @Override
        public boolean matches(PagingResponse<DataPackage> value) {
          return Objects.nonNull(value) && !value.getResults().isEmpty();
        }
      });
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
      assertThat(newDataPackage).isEqualTo(buildTestPackage(newDataPackage.getDoi().getUrl()));
      //verify that exists method was invoked
      verify(DATA_PACKAGE_MAPPER).create(any(DataPackage.class));
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
                 .request().header(HttpHeaders.AUTHORIZATION, TEST_USER_CREDENTIALS).delete().getStatus())
      .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    verify(DATA_PACKAGE_MAPPER).get(any(String.class));
    verify(DATA_PACKAGE_MAPPER).delete(any(DOI.class));
  }

  /**
   * Builds a test instance using configured test resources.
   */
  public DataPackage buildTestPackage(URI doiUrl) {
    DOI doi = new DOI(doiUrl.toString());
    DataPackage dataPackage = new DataPackage(uriBuilder.build(Paths.get(doiUrl.getPath()).getFileName().toString())
                                                .toString());
    dataPackage.setMetadata(METADATA_TEST_FILE);
    dataPackage.addFile(CONTENT_TEST_FILE);
    dataPackage.setDoi(doi);
    return dataPackage;
  }
}
