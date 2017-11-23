package org.gbif.datarepo.identifiers.validation;

import org.gbif.datarepo.api.model.Creator;

public class IdentifierSchemaValidatorFactory {

  private static final IdentifierSchemeValidator ORCID_VALIDATOR = new OrcidValidator();

  private static final IdentifierSchemeValidator ISNI_VALIDATOR = new IsniValidator();

  /**
   * Private constructor.
   */
  private IdentifierSchemaValidatorFactory() {
    //do nothing
  }

  public static IdentifierSchemeValidator create(Creator.IdentifierScheme identifierScheme) {
    if (Creator.IdentifierScheme.ORCID == identifierScheme) {
      return ORCID_VALIDATOR;
    }
    if (Creator.IdentifierScheme.ISNI == identifierScheme) {
      return ISNI_VALIDATOR;
    }
    throw new IllegalArgumentException("IdentifierScheme not supported");
  }
}
