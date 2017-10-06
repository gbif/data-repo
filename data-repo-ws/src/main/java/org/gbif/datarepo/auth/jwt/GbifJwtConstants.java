package org.gbif.datarepo.auth.jwt;

/**
 * Common constants used for GBIF JWT authentication.
 */
public class GbifJwtConstants {

  //JWT field that contains the user name.
  public static final String JWT_USER_NAME = "userName";
  /**
   * Security cookie name.
   */
  public static final String SECURITY_COOKIE = "token";
  /**
   * Security context name, used for informational purposes only.
   */
  static final String JWT_SECURITY_CONTEXT = "JWT";

  /**
   * Making constructor private.
   */
  private GbifJwtConstants() {
    //do nothing
  }

}
