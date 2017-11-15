package org.gbif.datarepo.resource;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.app.DataRepoConfigurationDW;
import org.gbif.datarepo.auth.basic.BasicAuthenticator;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.persistence.mappers.CreatorMapper;
import org.gbif.datarepo.persistence.mappers.IdentifierMapper;
import org.gbif.datarepo.persistence.mappers.BaseMapperTest;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;
import org.gbif.datarepo.store.fs.conf.DataRepoConfiguration;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.store.fs.FileSystemRepository;
import org.gbif.datarepo.test.mocks.DoiRegistrationServiceMock;
import org.gbif.doi.service.DoiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.validation.Validation;
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
import org.apache.bval.BeanValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Condition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hibernate.validator.internal.engine.ValidatorImpl;
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
import static org.gbif.datarepo.resource.PathsParams.RELATED_IDENTIFIERS_PATH;
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

  private static final GenericType<PagingResponse<DataPackage>> GENERIC_DATA_PACKAGE_PAGING_RESPONSE =
    new GenericType<PagingResponse<DataPackage>>(){};

  private static final GenericType<PagingResponse<Identifier>> GENERIC_RELATED_IDENTIFIER_PAGING_RESPONSE =
    new GenericType<PagingResponse<Identifier>>(){};

  //Mock authenticator, it always authenticates against the same user
  private static final BasicAuthenticator AUTHENTICATOR = mock(BasicAuthenticator.class);

  private static DataPackage testDataPackage;

  //Temporary local folder to store files generated for test cases
  private static Path temporaryFolder;

  //Configuration instance
  private static DataRepoConfigurationDW configuration;

  //Persistence layer injector
  private static Injector mappersInjector;

  /**
   * Guice Injector that provides instances of MyBatis Mappers.
   */
  private static Injector mappersInjector() {
    try {
      //Has the embedded DB been initiated
      if (!hasInitiated()) {
        initDB();
      }
      //Build the persistence layer injector
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
                                                                  mappersInjector().getInstance(IdentifierMapper.class),
                                                                  mappersInjector().getInstance(CreatorMapper.class),
                                                                  configuration().getDataRepoConfiguration()
                                                                    .getFileSystem()),
                                         configuration(), Validation.buildDefaultValidatorFactory().getValidator()))
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
    tearDown();
  }

  /**
   * Executed before each test case. Initializes all mock objects and create a test data package (testDataPackage).
   */
  @Before
  public void setup() {
    try {
      //Only the TEST_USER is valid
      when(AUTHENTICATOR.authenticate(Matchers.eq(TEST_BASIC_CREDENTIALS))).thenReturn(Optional.of(TEST_USER));
      clearDB();
      //Create test package
      testDataPackage = createTestDataPackage();
    } catch (Exception  ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Executed after each test, resets all the mock instances.
   */
  @After
  public void tearDownTestCase() {
    // we have to reset the mock after each test because from the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(AUTHENTICATOR);
  }

  /**
   * Tests that the content from directory 'testrepo/10.5072-dp.bvmv02' is retrieved correctly as DataPackage instance.
   */
  @Test
  public void testGetDataPackage() throws IOException  {
    //Data package read from test file
    DataPackage testDataPackage = resource.getObjectMapper().readValue(new File(JSON_CREATE_TEST_FILE),
                                                                       DataPackage.class);
    //Retrieves a data package instance using the test data package key
    DataPackage dataPackage = resource.getJerseyTest()
                                .target(Paths.get(DATA_PACKAGES_PATH, DataPackageResourceTest.testDataPackage.getKey().toString()).toString())
                                .request().get(DataPackage.class);
    //Have the same tag values
    assertThat(testDataPackage.getTags().stream()
                 .allMatch(testTag -> dataPackage.getTags().stream()
                   .anyMatch(tag -> tag.getValue().equals(testTag.getValue()))));

    //Have the same related identifiers
    assertThat(testDataPackage.getRelatedIdentifiers().stream()
                 .allMatch(testIdentifier -> dataPackage.getRelatedIdentifiers().stream()
                   .anyMatch(identifier -> identifier.getIdentifier().equals(testIdentifier.getIdentifier())
                                           && identifier.getType() == testIdentifier.getType()
                                           && identifier.getRelationType() == testIdentifier.getRelationType())
                 ));

    //Have the same creators
    assertThat(testDataPackage.getCreators().stream()
                 .allMatch(testCreator -> dataPackage.getCreators().stream()
                   .anyMatch(creator -> creator.getName().equals(testCreator.getName())
                                        && creator.getIdentifier().equals(testCreator.getIdentifier())
                                        && creator.getIdentifierScheme() == testCreator.getIdentifierScheme()
                                        && creator.getAffiliation().stream()
                                          .allMatch(affiliation -> testCreator.getAffiliation().contains(affiliation))
                   )));
  }


  /**
   * Tests the listing of data packages, filtered by user and tag.
   */
  @Test
  public void testListDataPackage()  {
    assertThat(resource.getJerseyTest().target(DATA_PACKAGES_PATH)
                 .queryParam("user", testDataPackage.getCreatedBy())
                 .queryParam("tag", testDataPackage.getTags().iterator().next().getValue())
                 .request().get(GENERIC_DATA_PACKAGE_PAGING_RESPONSE))
      .has(new Condition<PagingResponse<DataPackage>>() {
        @Override
        public boolean matches(PagingResponse<DataPackage> value) {
          return Objects.nonNull(value) && !value.getResults().isEmpty();
        }
      });
  }

  /**
   * Test the listing of related identifier of data package.
   */
  @Test
  public void testListIdentifiers()  {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, testDataPackage.getKey().toString(),
                                                         RELATED_IDENTIFIERS_PATH).toString())
                 .queryParam("user", testDataPackage.getCreatedBy())
                 .queryParam("relationType", Identifier.RelationType.IsAlternativeOf)
                 .request().get(
      GENERIC_RELATED_IDENTIFIER_PAGING_RESPONSE))
      .has(new Condition<PagingResponse<Identifier>>() {
        @Override
        public boolean matches(PagingResponse<Identifier> value) {
          return Objects.nonNull(value) && !value.getResults().isEmpty();
        }
      });
  }

  /**
   * Utility method that creates and instance of a Test data package.
   */
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
   * Test that the file 'occurrence.txt' can be retrieved.
   */
  @Test
  public void testGetFile() throws IOException {
    try (InputStream downloadFile = resource.getJerseyTest()
                                      .target(Paths.get(DATA_PACKAGES_PATH, testDataPackage.getKey().toString(),
                                                        CONTENT_TEST_FILE).toString())
                                      .request().get(InputStream.class); //download file
         //Read test file
        InputStream contentFile = new FileInputStream(Paths.get(TEST_DATA_PACKAGE_DIR, CONTENT_TEST_FILE).toFile())) {
        //compare file contents
        assertThat(downloadFile).hasSameContentAs(contentFile);
    }
  }

  /**
   * Tests that a DataPackage can be deleted.
   */
  @Test
  public void testDeleteDataPackage() throws DoiException {
    assertThat(resource.getJerseyTest().target(Paths.get(DATA_PACKAGES_PATH, testDataPackage.getKey().toString())
                                                 .toString())
                 .request().header(HttpHeaders.AUTHORIZATION, TEST_USER_CREDENTIALS).delete().getStatus())
      .isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

}
