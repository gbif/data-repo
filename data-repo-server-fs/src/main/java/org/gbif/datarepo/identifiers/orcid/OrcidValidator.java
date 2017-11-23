package org.gbif.datarepo.identifiers.orcid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrcidValidator {

  private static final Pattern ORCID_REGEX = Pattern.compile("^(https?:\\/\\/orcid.org\\/)?(([0-9]{4}-){3}([0-9]{3}[0-9X]{1}))$");

   public static boolean isValid(String orcid) {
     Matcher matcher = ORCID_REGEX.matcher(orcid);
     return matcher.matches() && hasValidChecksumDigit(orcid);
   }


  public static boolean hasValidChecksumDigit(String orcId) {
    return orcId.charAt(orcId.length() - 1) == generateChecksumDigit(orcId.substring(0, orcId.length() - 1));
  }

  /**
   * Generates check digit as per ISO 7064 11,2.
   *
   */
  public static char generateChecksumDigit(String baseDigits) {
    int total = 0;
    for (int i = 0; i < baseDigits.length(); i++) {
      int digit = Character.getNumericValue(baseDigits.charAt(i));
      total = (total + digit) << 1;
    }
    int remainder = total % 11;
    int result = (12 - remainder) % 11;
    return result == 10 ? 'X' : Character.forDigit(result, 10);
  }


  public static void main(String[] args) {
    System.out.println(OrcidValidator.isValid("0000-0001-2345-6785"));
  }
}
