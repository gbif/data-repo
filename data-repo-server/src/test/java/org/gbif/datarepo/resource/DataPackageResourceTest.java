package org.gbif.datarepo.resource;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.conf.DataCiteConfiguration;
import org.gbif.datarepo.conf.DataRepoConfiguration;
import org.gbif.datarepo.conf.DbConfiguration;
import org.gbif.datarepo.datacite.DataPackagesDoiGenerator;
import org.gbif.datarepo.model.DataPackage;
import org.gbif.datarepo.model.DataPackageDeSerTest;
import org.gbif.datarepo.store.DataRepository;
import org.gbif.doi.service.datacite.DataCiteService;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

public class DataPackageResourceTest {


  private static final DataCiteService DATA_CITE_SERVICE = mock(DataCiteService.class);

  private static final DataPackagesDoiGenerator DOI_GENERATOR = mock(DataPackagesDoiGenerator.class);

  private static final DataRepository DATA_REPOSITORY = mock(DataRepository.class);

  private static final DataPackage TEST_DATA_PACKAGE = DataPackageDeSerTest.testDataPackage();

  private static DataRepoConfiguration configuration;

  @ClassRule
  public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

  //Grizzly is required since he in-memory Jersey test container does not support all features,
  // such as the @Context injection used by BasicAuthFactory and OAuthFactory.
  @ClassRule
  public static final ResourceTestRule RESOURCE = ResourceTestRule.builder()
    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
    .addResource(new DataPackageResource(DATA_REPOSITORY, DATA_CITE_SERVICE, DOI_GENERATOR, configuration))
    .addProvider(MultiPartFeature.class)
    .build();

  private static DataRepoConfiguration configuration() {
    try {
      DataRepoConfiguration configuration = new DataRepoConfiguration();
      configuration.setDoiShoulder("dp.");
      configuration.setDoiCommonPrefix("10.5072");
      configuration.setDataRepoPath(TEMPORARY_FOLDER.newFolder("datarepo").getPath());
      configuration.setGbifApiUrl("http://localhost:8080/data_packages/");
      configuration.setGbifPortalUrl("http://www.gbif-dev.org/datapackage/");
      DataCiteConfiguration dataCiteConfiguration = new DataCiteConfiguration();
      dataCiteConfiguration.setApiUrl("https://mds.datacite.org/");
      dataCiteConfiguration.setPassword("blah");
      dataCiteConfiguration.setUserName("DK.GBIF");
      configuration.setDataCiteConfiguration(dataCiteConfiguration);
      configuration.setUsersDb(mock(DbConfiguration.class));
      return configuration;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }


  @BeforeClass
  public static void init() {
    configuration = configuration();
  }

  @Before
  public void setup() {
    when(DATA_REPOSITORY.get(any(DOI.class))).thenReturn(Optional.of(Paths.get(configuration.getDataRepoPath())));
    when(DOI_GENERATOR.newDOI()).thenReturn(new DOI("10.5072", "dp.bvmv02"));
  }

  @After
  public void tearDown(){
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(DATA_REPOSITORY);
    reset(DOI_GENERATOR);
    reset(DATA_CITE_SERVICE);
  }

  @Test
  public void testGetDataPackage() {
    assertThat(RESOURCE.getJerseyTest().target("/data_package/dp.bvmv02").register(MultiPartFeature.class)
      .request().get(DataPackage.class))
      .isEqualTo(TEST_DATA_PACKAGE);
    verify(DATA_REPOSITORY).get(any(DOI.class));
  }

}
