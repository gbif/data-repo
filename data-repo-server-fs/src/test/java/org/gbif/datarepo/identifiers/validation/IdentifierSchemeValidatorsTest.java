package org.gbif.datarepo.identifiers.validation;


import static org.junit.Assert.assertEquals;

import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.validation.identifierschemes.IdentifierSchemaValidatorFactory;
import org.gbif.datarepo.api.validation.identifierschemes.IdentifierSchemeValidator;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Parameterized tests for Creators identifiers values.
 */
@RunWith(Parameterized.class)
public class IdentifierSchemeValidatorsTest {

  /**
   * Test data, see this class constructor to understand the
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      //valid OrcIds
      {"http://orcid.org/0000-0001-5473-3208", Creator.IdentifierScheme.ORCID, true,
        "https://orcid.org/0000-0001-5473-3208"},
      {"https://orcid.org/0000-0001-5473-3208", Creator.IdentifierScheme.ORCID, true,
        "https://orcid.org/0000-0001-5473-3208"},
      {"0000-0001-5473-3208", Creator.IdentifierScheme.ORCID, true,
        "https://orcid.org/0000-0001-5473-3208"},

      //valid ISNI numbers
      {"0000-0001-5473-3208", Creator.IdentifierScheme.ISNI, true, "0000-0001-5473-3208"},
      {"0000-0001-5473-3208", Creator.IdentifierScheme.ISNI, true, "0000-0001-5473-3208"},
      {"  000000007359228X  ", Creator.IdentifierScheme.ISNI, true, "000000007359228X"},

      //non-valid Orcids
      {"https://orcid.org/0000-0001-5473-3206", Creator.IdentifierScheme.ORCID, false, null},
      {"http://orcid.org/0000-0001-5473-3206", Creator.IdentifierScheme.ORCID, false, null},
      {"0000-0001-5473-3206", Creator.IdentifierScheme.ORCID, false, null},
      {"0000-3206", Creator.IdentifierScheme.ORCID, false, null},
      {"0000-3206-2-12", Creator.IdentifierScheme.ORCID, false, null},
      {"0000000154733208", Creator.IdentifierScheme.ORCID, false, null}, //Orcids require '-'

      //non-valid ISNI numbers
      {"0000-0001-5473-3278", Creator.IdentifierScheme.ISNI, false, null},
      {"0000 0000 7359 226X", Creator.IdentifierScheme.ISNI, false, null},
      {"000000007359221X", Creator.IdentifierScheme.ISNI, false, null},
    });
  }

  //Identifier to be tested
  private final String identifier;

  //Identifier scheme type
  private final Creator.IdentifierScheme identifierScheme;

  //Test case expected validation result result
  private final boolean expectedValidationResult;

  //Test case expected validation result result
  private final String expectedNormalizationResult;

  /**
   * Constructor used for parametric test cases.
   */
  public IdentifierSchemeValidatorsTest(String identifier, Creator.IdentifierScheme identifierScheme,
                                        boolean expectedValidationResult, String expectedNormalizationResult) {
    this.identifier = identifier;
    this.identifierScheme = identifierScheme;
    this.expectedValidationResult = expectedValidationResult;
    this.expectedNormalizationResult = expectedNormalizationResult;
  }

  /**
   * General test cases that validates a expected result from identifier validation.
   */
  @Test
  public void test() {
    IdentifierSchemeValidator validator = IdentifierSchemaValidatorFactory.getValidator(identifierScheme);
    assertEquals(validator.isValid(identifier), expectedValidationResult);
    if (expectedNormalizationResult != null) {
     assertEquals(validator.normalize(identifier), expectedNormalizationResult);
    }
  }
}
