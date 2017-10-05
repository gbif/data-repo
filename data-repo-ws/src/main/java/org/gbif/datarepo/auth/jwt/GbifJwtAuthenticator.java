package org.gbif.datarepo.auth.jwt;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

/**
 * Dropwizard authenticator that validates if the user has been authenticated against a Jason Web Token.
 * Once the JWT is decoded and property 'userName' is examined to determine if it represents a valid user.
 */
public class GbifJwtAuthenticator implements Authenticator<String, GbifUserPrincipal> {

  //JWT field that contains the user name.
  private static final String JWT_USER_NAME = "userName";

  //Private secure key used to encode/decode the JWT.
  private final byte[] jwtSigningKey;

  //GBIF users service
  private final IdentityAccessService identityAccessService;

  /**
   * Default/Full constructor.
   * @param jwtSigningKey key to encode and decode tokens
   * @param identityAccessService GBIF identity service
   */
  public GbifJwtAuthenticator(String jwtSigningKey, IdentityAccessService identityAccessService) {
    this.jwtSigningKey= jwtSigningKey.getBytes();
    this.identityAccessService = identityAccessService;
  }

  /**
   * Authenticates a GBIF user represented as encoded JWT token in the credentials parameter.
   * @param credentials encoded string containing the JWT token
   * @return a GbifUserPrincipal if present
   * @throws AuthenticationException in case of error
   */
  @Override
  public Optional<GbifUserPrincipal> authenticate(String credentials) throws AuthenticationException {
    Jws<Claims> jws = Jwts.parser().setSigningKey(jwtSigningKey).parseClaimsJws(credentials);
    return Optional.fromNullable(jws.getBody().get(JWT_USER_NAME, String.class))
            .transform(identityAccessService::get)
            .transform(GbifUserPrincipal::new);
  }
}
