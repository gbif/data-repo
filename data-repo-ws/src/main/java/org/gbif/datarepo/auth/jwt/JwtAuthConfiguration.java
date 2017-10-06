package org.gbif.datarepo.auth.jwt;

import javax.validation.constraints.NotNull;

/**
 * Common constants used for GBIF JWT authentication.
 */
public class JwtAuthConfiguration {

  /**
   * Default user name filed in the JWT body.
   */
  public static final String DEFAULT_JWT_USER_NAME = "userFieldName";

  /**
   * Default security cookie name.
   */
  public static final String DEFAULT_SECURITY_COOKIE = "token";

  /**
   * Default security context.
   */
  static final String DEFAULT_JWT_SECURITY_CONTEXT = "JWT";


  @NotNull
  private String signingKey;

  @NotNull
  private String cookieName = DEFAULT_SECURITY_COOKIE;

  @NotNull
  private String userFieldName = DEFAULT_JWT_USER_NAME;

  @NotNull
  private String securityContext = DEFAULT_JWT_SECURITY_CONTEXT;

  /**
   * Jason Web Token used to trust in externally authenticated users.
   */
  public String getSigningKey() {
    return signingKey;
  }

  public void setSigningKey(String signingKey) {
    this.signingKey = signingKey;
  }

  /**
   * JWT token/cookie name.
   */
  public String getCookieName() {
    return cookieName;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  /**
   * Field name that contains the username value in the JWT body.
   */
  public String getUserFieldName() {
    return userFieldName;
  }

  public void setUserFieldName(String userFieldName) {
    this.userFieldName = userFieldName;
  }

  /**
   * Security context name, used for informational purposes only.
   */
  public String getSecurityContext() {
    return securityContext;
  }

  public void setSecurityContext(String securityContext) {
    this.securityContext = securityContext;
  }
}
