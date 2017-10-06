package org.gbif.datarepo.auth.basic;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication service.
 * Performs a basic authentication (user and password) against the GBUIF user service.
 */
public class BasicAuthenticator implements Authenticator<BasicCredentials, GbifUserPrincipal> {

  private static final Logger LOG = LoggerFactory.getLogger(BasicAuthenticator.class);

  //GBIF Security realm
  public static final String GBIF_REALM = "GBIF";

  //For logging purposes only
  private static final String SCHEME = "BASIC";

  //GBIF users service
  private final IdentityAccessService identityAccessService;

  /**
   * Default constructor, requires a UserService instance.
   * @param identityAccessService GBIF user service
   */
  public BasicAuthenticator(IdentityAccessService identityAccessService) {
    this.identityAccessService = identityAccessService;
  }

  /**
   * Performs the authentication against the GBIF UserService.
   */
  @Override
  public Optional<GbifUserPrincipal> authenticate(BasicCredentials credentials) throws AuthenticationException {
    GbifUser user = identityAccessService.authenticate(credentials.getUsername(), credentials.getPassword());
    if (user != null) { //User found
      LOG.debug("Authenticating user {} via scheme {}", credentials.getUsername(), SCHEME);
      return Optional.of(new GbifUserPrincipal(user));
    }
    //User not found
    LOG.debug("User {} not found via scheme {}", credentials.getUsername(), SCHEME);
    return Optional.absent();
  }

}
