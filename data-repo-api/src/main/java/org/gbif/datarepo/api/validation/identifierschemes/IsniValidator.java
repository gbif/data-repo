package org.gbif.datarepo.api.validation.identifierschemes;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Validator for ISNI numbers.
 */
public class IsniValidator implements IdentifierSchemeValidator {

  private static final Pattern PATTERN = Pattern.compile("[\\p{Digit}xX\\p{Pd}\\s]{16,24}");

  @Override
  public boolean isValid(String value) {
    return !Strings.isNullOrEmpty(value) && PATTERN.matcher(value).matches() && Mod112.hasValidChecksumDigit(value);
  }

  @Override
  public String normalize(String value) {
    Preconditions.checkNotNull(value, "Identifier value can't be null");
    return value.trim();
  }

}
