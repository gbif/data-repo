package org.gbif.datarepo.identifiers.validation;

import org.gbif.datarepo.api.model.Creator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Validator for Orcid identifiers.
 */
public class OrcidValidator implements IdentifierSchemeValidator {

  private static final int ORCID_NORM_GROUPS = 5;
  private static final Pattern ORCID_PATTERN = Pattern.compile("^(https?:\\/\\/orcid.org\\/)?(([0-9]{4}-){3}([0-9]{3}[0-9X]{1}))$");

  @Override
   public boolean isValid(String value) {
     Matcher matcher = ORCID_PATTERN.matcher(value);
     return matcher.matches() && IdentifierSchemeValidator.hasValidChecksumDigit(value);
   }

  @Override
  public String normalize(String value) {
    Matcher matcher = ORCID_PATTERN.matcher(value);
    if(matcher.matches()) {
      if (ORCID_NORM_GROUPS == matcher.groupCount()) {
        return value;
      } else if (ORCID_NORM_GROUPS - 1 == matcher.groupCount()) {
        return Creator.IdentifierScheme.ORCID.getSchemeURI() + '/' + value;
      }
    }
    throw new IllegalArgumentException(value + " it not a valid orcid");
  }



}
