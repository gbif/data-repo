package org.gbif.datarepo.api.model;
import org.gbif.api.model.common.DOI;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests serialization and deserialization of DataPackage instances.
 */
public class DataPackageDeSerTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  // Relative path to the json test file
  private static final String DP_JSON_TEST_FILE = "fixtures/datapackage.json";

  public static final String TEST_DOI_SUFFIX = "dp.bvmv02";

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
   * Creates an instance of DataPackage that matches the definition stored in 'fixtures/datapackage.json'.
   */
  public static DataPackage testDataPackage() {
    DataPackage dataPackage = new DataPackage("http://localhost:8080/data_packages/" + TEST_DOI_SUFFIX + '/');
    dataPackage.setMetadata("metadata.xml");
    dataPackage.addFile("occurrence.txt");
    dataPackage.setDoi(new DOI(DOI.TEST_PREFIX, TEST_DOI_SUFFIX));
    return  dataPackage;
  }

}
