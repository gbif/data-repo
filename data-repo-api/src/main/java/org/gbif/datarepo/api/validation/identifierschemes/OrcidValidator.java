package org.gbif.datarepo.api.validation.identifierschemes;

import org.gbif.datarepo.api.model.Creator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 *  Validator for Orcid identifiers.
 */
public class OrcidValidator implements IdentifierSchemeValidator {

  private static final int ORCID_NORM_GROUPS = 5;
  private static final Pattern ORCID_PATTERN = Pattern.compile("^(?<scheme>http(?:s)?:\\/\\/orcid.org\\/)?(([0-9]{4}-){3}([0-9]{3}[0-9X]{1}))$");

  @Override
  public boolean isValid(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return false;
    }
    Matcher matcher = ORCID_PATTERN.matcher(value);
    return matcher.matches() && Mod112.hasValidChecksumDigit(withoutScheme(matcher, value));
  }

  @Override
  public String normalize(String value) {
    Preconditions.checkNotNull(value, "Identifier value can't be null");
    String trimmedValue = value.trim();
    Matcher matcher = ORCID_PATTERN.matcher(trimmedValue);
    if (matcher.matches()) {
      return Creator.IdentifierScheme.ORCID.getSchemeURI() + '/' + withoutScheme(matcher, trimmedValue);
    }
    throw new IllegalArgumentException(value + " it not a valid Orcid");
  }

  /**
   * This
   */
  private static String withoutScheme(Matcher matcher, String value) {
    String schemeGroup = matcher.group("scheme");
    return schemeGroup == null ? value : value.substring(schemeGroup.length());
  }

}
