package org.gbif.datarepo.auth.jwt;

import org.gbif.api.model.common.GbifUserPrincipal;

import java.io.IOException;
import java.security.Principal;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

/**
 * Authenticates an GBIF user from an encoded JWT token.
 */
public class GbifJwtCredentialsFilter extends AuthFilter<String, GbifUserPrincipal> {

  private final GbifAuthJwtConfiguration configuration;

  public GbifJwtCredentialsFilter(GbifAuthJwtConfiguration configuration) {
    this.configuration = configuration;
  }
  /**
   * If the cookie (SECURITY_COOKIE) is present, it is validated agains the provided authenticator.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    java.util.Optional.ofNullable(requestContext.getCookies().get(configuration.getCookieName()))
      .ifPresent(cookie -> {
                      try {
                        Optional<GbifUserPrincipal> principal = authenticator.authenticate(cookie.getValue());
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
    );
  }

  /**
   * Builder for {@link GbifJwtCredentialsFilter}.
   * <p>An {@link Authenticator} must be provided during the building process.</p>
   */
  public static class Builder extends AuthFilterBuilder<String, GbifUserPrincipal, GbifJwtCredentialsFilter> {

    private GbifAuthJwtConfiguration configuration;

    /**
     * Sets the given configuration.
     *
     * @param configuration a configuration
     * @return the current builder
     */
    public AuthFilterBuilder<String, GbifUserPrincipal, GbifJwtCredentialsFilter> setConfiguration(GbifAuthJwtConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }


    @Override
    protected GbifJwtCredentialsFilter newInstance() {
      Preconditions.checkNotNull(configuration, "Configuration can't be null");
      return new GbifJwtCredentialsFilter(configuration);
    }
  }
}
