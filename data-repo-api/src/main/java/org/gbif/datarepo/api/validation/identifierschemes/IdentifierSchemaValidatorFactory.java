package org.gbif.datarepo.api.validation.identifierschemes;

import org.gbif.datarepo.api.model.Creator;

/**
 * Factory class of identifier schemes.
 */
public class IdentifierSchemaValidatorFactory {

  private static final IdentifierSchemeValidator ORCID_VALIDATOR = new OrcidValidator();

  private static final IdentifierSchemeValidator ISNI_VALIDATOR = new IsniValidator();

  private static final IdentifierSchemeValidator OTHER_VALIDATOR = new OtherValidator();

  /**
   * Private constructor.
   */
  private IdentifierSchemaValidatorFactory() {
    //do nothing
  }

  /**
   * Factory method.
   * @param identifierScheme scheme type
   * @return a instance of IdentifierSchemeValidator that validates the IdentifierScheme
   */
  public static IdentifierSchemeValidator getValidator(Creator.IdentifierScheme identifierScheme) {
    if (Creator.IdentifierScheme.ORCID == identifierScheme) {
      return ORCID_VALIDATOR;
    }
    if (Creator.IdentifierScheme.ISNI == identifierScheme) {
      return ISNI_VALIDATOR;
    }
    if (Creator.IdentifierScheme.OTHER == identifierScheme) {
      return OTHER_VALIDATOR;
    }
    throw new IllegalArgumentException("IdentifierScheme not supported");
  }
}
