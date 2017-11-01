package org.gbif.datarepo.auth.jwt;

import org.gbif.api.model.common.GbifUserPrincipal;

import java.io.IOException;
import java.security.Principal;
import java.util.regex.Pattern;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

/**
 * Authenticates an GBIF user from an encoded JWT token.
 */
public class JwtCredentialsFilter extends AuthFilter<String, GbifUserPrincipal> {

  private final JwtAuthConfiguration configuration;

  //Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PAT =  Pattern.compile("(?i)bearer");

  public JwtCredentialsFilter(JwtAuthConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * If the cookie (SECURITY_COOKIE) is present, it is validated agains the provided authenticator.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    //Tries to read the authorization token form the header
    java.util.Optional<String> authHeader = java.util.Optional
      .ofNullable(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
    if (authHeader.isPresent()) {
      authenticate(removeBearer(authHeader.get()), requestContext);
    } else { //if the authorization header is not present it tries to read a cookie named 'token'
      java.util.Optional.ofNullable(requestContext.getCookies().get(configuration.getCookieName()))
        .ifPresent(cookie -> authenticate(cookie.getValue(), requestContext));
    }
  }

  /**
   * Removes 'bearer' token, leading an trailing whitespaces.
   * @param token to be clean
   * @return a token without whitespaces and the word 'bearer'
   */
  private static String removeBearer(String token) {
    return BEARER_PAT.matcher(token).replaceAll("").trim();
  }

  /**
   * Authenticates using the provided credentials.
   * @param credentials user jwt credentials
   * @param requestContext context being filtered
   */
  private void authenticate(String credentials, ContainerRequestContext requestContext) {
    try {
      Optional<GbifUserPrincipal> principal = authenticator.authenticate(credentials);
      if (principal.isPresent()) {
        requestContext.setSecurityContext(new SecurityContext() {
          @Override
          public Principal getUserPrincipal() {
            return principal.get();
          }

          @Override
          public boolean isUserInRole(String role) {
            return authorizer.authorize(principal.get(), role);
          }

          @Override
          public boolean isSecure() {
            return requestContext.getSecurityContext().isSecure();
          }

          @Override
          public String getAuthenticationScheme() {
            return configuration.getSecurityContext();
          }
        });
      } else {
        throw new NotAuthorizedException("Invalid UserName in JWT token");
      }
    } catch (AuthenticationException ex) {
      throw new InternalServerErrorException(ex);
    }
  }

  /**
   * Builder for {@link JwtCredentialsFilter}.
   * <p>An {@link Authenticator} must be provided during the building process.</p>
   */
  public static class Builder extends AuthFilterBuilder<String, GbifUserPrincipal, JwtCredentialsFilter> {

    private JwtAuthConfiguration configuration;

    /**
     * Sets the given configuration.
     *
     * @param configuration a configuration
     * @return the current builder
     */
    public AuthFilterBuilder<String, GbifUserPrincipal, JwtCredentialsFilter> setConfiguration(JwtAuthConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }


    @Override
    protected JwtCredentialsFilter newInstance() {
      Preconditions.checkNotNull(configuration, "Configuration can't be null");
      return new JwtCredentialsFilter(configuration);
    }
  }
}
