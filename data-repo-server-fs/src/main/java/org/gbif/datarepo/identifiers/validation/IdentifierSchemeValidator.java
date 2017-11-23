package org.gbif.datarepo.identifiers.validation;

/**
 * Validation interface of Creator.IdentifierScheme values.
 */
public interface IdentifierSchemeValidator {

  /**
   * Is the
   */
  boolean isValid(String value);

  String normalize(String value);


  static boolean hasValidChecksumDigit(String value) {
    return value.charAt(value.length() - 1) ==
           generateChecksumDigit(value.substring(0, value.length() - 1));
  }

  /**
   * Generates check digit as per ISO 7064 11,2.
   *
   */
  static char generateChecksumDigit(String baseDigits) {
    int total = 0;
    for (int i = 0; i < baseDigits.length(); i++) {
      int digit = Character.getNumericValue(baseDigits.charAt(i));
      total = (total + digit) << 1;
    }
    int remainder = total % 11;
    int result = (12 - remainder) % 11;
    return result == 10 ? 'X' : Character.forDigit(result, 10);
  }

}
