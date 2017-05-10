package org.gbif.datarepo.api.model;
import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.store.fs.FileSystemRepository;

import java.io.File;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.datarepo.test.utils.ResourceTestUtils.CONTENT_TEST_FILE;
import static org.gbif.datarepo.test.utils.ResourceTestUtils.TEST_DATA_PACKAGE_DIR;

/**
 * Tests serialization and deserialization from DataPackage instances.
 */
public class DataPackageDeSerTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  // Relative path to the json test file
  private static final String DP_JSON_TEST_FILE = "fixtures/datapackage.json";

  public static final String TEST_DOI_SUFFIX = "dp.bvmv02";

  private static final DataPackageFile OCCURRENCE_TEST_FILE =
    new DataPackageFile("occurrence.txt",
                        FileSystemRepository.md5(new File(TEST_DATA_PACKAGE_DIR + CONTENT_TEST_FILE)),
                        18644);

  /**
   * Test that a DataPackage instance java-created is equals to an instance obtained from 'fixtures/datapackage.json'.
   */
  @Test
  public void serializesToJSON() throws Exception {
    String expected = MAPPER.writeValueAsString(
      MAPPER.readValue(fixture(DP_JSON_TEST_FILE), DataPackage.class));

    assertThat(MAPPER.writeValueAsString(testDataPackage())).isEqualTo(expected);
  }

  /**
   * Test that a DataPackage instance obtained from 'fixtures/datapackage.json' is equals to a java-created instance.
   */
  @Test
  public void deserializesFromJSON() throws Exception {
    assertThat(MAPPER.readValue(fixture(DP_JSON_TEST_FILE), DataPackage.class))
      .isEqualTo(testDataPackage());
  }

  /**
   * Creates an instance from DataPackage that matches the definition stored in 'fixtures/datapackage.json'.
   */
  public static DataPackage testDataPackage() {
    return  testDataPackage(TEST_DOI_SUFFIX);
  }

  /**
   * Creates an instance from DataPackage that matches the definition stored in 'fixtures/datapackage.json'.
   */
  public static DataPackage testDataPackage(String doiSuffix) {
    DataPackage dataPackage = new DataPackage("http://localhost:8080/data_packages/" + doiSuffix + '/');
    dataPackage.setMetadata("metadata.xml");
    dataPackage.addFile(OCCURRENCE_TEST_FILE);
    dataPackage.setTitle("Test Title");
    dataPackage.setDescription("Test Description");
    dataPackage.setDoi(new DOI(DOI.TEST_PREFIX, doiSuffix));
    dataPackage.setChecksum("ae5e1348dd9f8e9ec1d6f1b14937a4d4");
    dataPackage.setSize(18644);
    return  dataPackage;
  }

}
