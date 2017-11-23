package org.gbif.datarepo.auth.jwt;

import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;

import java.util.Optional;
import javax.ws.rs.NotAuthorizedException;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Dropwizard authenticator that validates if the user has been authenticated against a Jason Web Token.
 * Once the JWT is decoded and property 'userName' is examined to determine if it represents a valid user.
 */
public class JwtAuthenticator implements Authenticator<String, GbifUserPrincipal> {

  //Private secure key used to encode/decode the JWT.
  private final byte[] jwtSigningKey;

  //GBIF users service
  private final IdentityAccessService identityAccessService;

  private final JwtAuthConfiguration configuration;

  /**
   * Default/Full constructor.
   * @param configuration JWT configuration
   * @param identityAccessService GBIF identity service
   */
  public JwtAuthenticator(JwtAuthConfiguration configuration, IdentityAccessService identityAccessService) {
    this.configuration= configuration;
    this.identityAccessService = identityAccessService;
    jwtSigningKey = configuration.getSigningKey().getBytes();
  }

  /**
   * Authenticates a GBIF user represented as encoded JWT token in the credentials parameter.
   * @param credentials encoded string containing the JWT token
   * @return a GbifUserPrincipal if present   or NotAuthorizedException if the credential are invalid
   * @throws AuthenticationException in case of error
   */
  @Override
  public Optional<GbifUserPrincipal> authenticate(String credentials) throws AuthenticationException {
    try {
      Jws<Claims> jws = Jwts.parser().setSigningKey(jwtSigningKey)
        .parseClaimsJws(credentials);
      return Optional.ofNullable(jws.getBody().get(configuration.getUserFieldName(), String.class))
        .map(identityAccessService::get)
        .map(GbifUserPrincipal::new);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new NotAuthorizedException("Invalid JWT token", ex);
    } catch (Exception ex) {
      throw new AuthenticationException("JWT token could not be verified", ex);
    }
  }
}
