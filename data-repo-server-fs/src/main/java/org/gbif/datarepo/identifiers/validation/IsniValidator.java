package org.gbif.datarepo.identifiers.validation;

import java.util.regex.Pattern;

/**
 * Validator for ISNI numbers.
 */
public class IsniValidator implements IdentifierSchemeValidator {

  private static final Pattern PATTERN = Pattern.compile("[\\p{Digit}xX\\p{Pd}\\s]{16,24}");

  private static final Pattern NORMALIZE_PATTERN = Pattern.compile("-|\\s+");

  @Override
  public boolean isValid(String value) {
    return PATTERN.matcher(value).matches() && IdentifierSchemeValidator.hasValidChecksumDigit(value);
  }

  @Override
  public String normalize(String value) {
    return NORMALIZE_PATTERN.matcher(value).replaceAll("");
  }

}
