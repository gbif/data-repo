package org.gbif.datarepo.resource;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.app.DataRepoConfigurationDW;
import org.gbif.datarepo.auth.basic.BasicAuthenticator;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.persistence.mappers.AlternativeIdentifierMapper;
import org.gbif.datarepo.persistence.mappers.BaseMapperTest;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.test.mocks.DoiRegistrationServiceMock;
import org.gbif.doi.service.DoiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.inject.Injector;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
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
import static org.mockito.Mockito.mock;

import static org.gbif.datarepo.resource.PathsParams.DATA_PACKAGES_PATH;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_DATA_PACKAGE_DIR;
import static org.gbif.datarepo.resource.PathsParams.FILE_PARAM;
import static org.gbif.datarepo.resource.PathsParams.DP_FORM_PARAM;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.CONTENT_TEST_FILE;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.JSON_CREATE_TEST_FILE;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_USER;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_USER_CREDENTIALS;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_BASIC_CREDENTIALS;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.dataBodyPartOf;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.dataBodyPartOfContent;

/**
 * Test class for the DataPackageResource class.
 * Implements test cases for: data packages creation, retrieval and deletion.
 */
public class DataPackageResourceTest extends BaseMapperTest {

  private static final GenericType<PagingResponse<DataPackage>> GENERIC_PAGING_RESPONSE =
    new GenericType<PagingResponse<DataPackage>>(){};

  private static final BasicAuthenticator AUTHENTICATOR = mock(BasicAuthenticator.class);

  private static DataPackage TEST_DATA_PACKAGE;

  private static Path temporaryFolder;

  private static DataRepoConfigurationDW configuration;

  private static Injector mappersInjector;

  private static Injector mappersInjector() {
    try {
      if (jdbcUrl == null) {
        initDB();
      }
      if (mappersInjector == null) {
        mappersInjector = buildInjector();
      }
      return mappersInjector;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }
  /**
   * Creates an temporary folder, the return instance is lazy evaluated into the field temporaryFolder.
   */
  private static Path temporaryFolder() {
    try {
      if (temporaryFolder == null) {
        //Creates the temporary folder using a random prefix
        temporaryFolder = Files.createTempDirectory(RandomStringUtils.randomAlphanumeric(3));
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
  private static DataRepoConfigurationDW configuration() {
    if (configuration == null) {
      configuration = new DataRepoConfigurationDW();
      DataRepoConfiguration dataRepoConfiguration = new DataRepoConfiguration();
      dataRepoConfiguration.setDoiCommonPrefix("10.5072");
      dataRepoConfiguration.setGbifApiUrl("http://localhost:8080/");
      dataRepoConfiguration.setDataPackageApiUrl("http://localhost:8080/data_packages/");
      //Used the temporary folder as the data repo path
      dataRepoConfiguration.setDataRepoPath("file://" + temporaryFolder() + '/');
      configuration.setDataRepoConfiguration(dataRepoConfiguration);
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
    .addProvider(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<GbifUserPrincipal>()
                                          .setAuthenticator(AUTHENTICATOR)
                                          .setRealm(BasicAuthenticator.GBIF_REALM).buildAuthFilter()))
    .addProvider(new AuthValueFactoryProvider.Binder<>(GbifUserPrincipal.class))
    //Test resource
    .addResource(new DataPackageResource(new FileSystemRepository(configuration().getDataRepoConfiguration()
                                                                    .getDataRepoPath(),
                                                                  new DoiRegistrationServiceMock(),
                                                                  mappersInjector().getInstance(DataPackageMapper.class),
                                                                  mappersInjector().getInstance(DataPackageFileMapper.class),
                                                                  mappersInjector().getInstance(TagMapper.class),
                                                                  mappersInjector().getInstance(RepositoryStatsMapper.class),
                                                                  mappersInjector().getInstance(AlternativeIdentifierMapper.class),
                                                                  configuration().getDataRepoConfiguration()
                                                                    .getFileSystem()),
                                         configuration()))
    .build();

  /**
   * Initialized all the elements used across all test cases.
   */
  @BeforeClass
  public static void init() throws  IOException {
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
      //Only the TEST_USER is valid
      when(AUTHENTICATOR.authenticate(Matchers.eq(TEST_BASIC_CREDENTIALS))).thenReturn(Optional.of(TEST_USER));
      //Create test package
      TEST_DATA_PACKAGE = createTestDataPackage();
    } catch (Exception  ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Executed after each test, resets all the mock instances.
   */
  @After
  public void tearDownTesCase() {
    // we have to reset the mock after each test because from the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(AUTHENTICATOR);
    clearDB();
  }

  /**
   * Tests that the content from directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testGetDataPackage() {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, TEST_DATA_PACKAGE.getKey().toString())
                                                 .toString())
                 .request().get(DataPackage.class))
      .isNotNull();
  }


  /**
   * Tests that the content from directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
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
      DataPackage newDataPackage = createTestDataPackage();
      //Test that both packages contains the same elements
      assertThat(newDataPackage.getKey()).isNotNull();
    assertThat(newDataPackage.getCreated()).isEqualTo(newDataPackage.getModified());
  }

  private static DataPackage createTestDataPackage() throws Exception {
    try (MultiPart multiPart = new FormDataMultiPart().bodyPart(dataBodyPartOfContent(JSON_CREATE_TEST_FILE, DP_FORM_PARAM))
      .bodyPart(dataBodyPartOf(TEST_DATA_PACKAGE_DIR + CONTENT_TEST_FILE, FILE_PARAM))) {
      return resource.getJerseyTest()
        .target(DATA_PACKAGES_PATH)
        .register(MultiPartFeature.class)
        .request()
        .header(HttpHeaders.AUTHORIZATION, TEST_USER_CREDENTIALS)
        .post(Entity.entity(multiPart, multiPart.getMediaType()), DataPackage.class);
    }
  }

  /**
   * Test that the file 'testrepo/10.5072-dp.bvmv02/occurrence.txt' can be retrieved.
   */
  @Test
  public void testGetFile() throws IOException {
    try (InputStream downloadFile = resource.getJerseyTest()
                                      .target(Paths.get(DATA_PACKAGES_PATH, TEST_DATA_PACKAGE.getKey().toString(),
                                                        CONTENT_TEST_FILE).toString())
                                      .request().get(InputStream.class); //download file
         //Read test file
        InputStream contentFile = new FileInputStream(Paths.get(TEST_DATA_PACKAGE_DIR, CONTENT_TEST_FILE).toFile())) {
        //compare file contents
        assertThat(downloadFile).hasSameContentAs(contentFile);
    }
  }

  /**
   * Tests that the content from directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testDeleteDataPackage() throws DoiException {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, TEST_DATA_PACKAGE.getKey().toString())
                                                 .toString())
                 .request().header(HttpHeaders.AUTHORIZATION, TEST_USER_CREDENTIALS).delete().getStatus())
      .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

}
