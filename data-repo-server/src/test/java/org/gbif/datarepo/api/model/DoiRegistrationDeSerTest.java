package org.gbif.datarepo.api.model;

import org.gbif.registry.doi.registration.DoiRegistration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static io.dropwizard.testing.FixtureHelpers.fixture;

/**
 * Tests serialization and deserialization of DataPackage instances.
 */
public class DoiRegistrationDeSerTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  // Relative path to the json test file
  private static final String DP_JSON_TEST_FILE = "fixtures/doiregistration.json";


  /**
   * Test that a DataPackage instance java-created is equals to an instance obtained from 'fixtures/datapackage.json'.
   */
  @Test
  public void serializesToJSON() throws Exception {
    String expected = MAPPER.writeValueAsString(MAPPER.readValue(fixture(DP_JSON_TEST_FILE), DoiRegistration.class));
    Assertions.assertThat(expected != null);
  }

}
