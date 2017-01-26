package org.gbif.datarepo.auth;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.api.service.common.UserService;

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
public class GbifAuthenticator implements Authenticator<BasicCredentials, UserPrincipal> {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthenticator.class);

  //For logging purposes only
  private static final String SCHEME = "BASIC";

  //GBIF users service
  private final UserService userService;

  /**
   * Default constructor, requires a UserService instance.
   * @param userService GBIF user service
   */
  public GbifAuthenticator(UserService userService) {
    this.userService = userService;
  }

  /**
   * Performs the authentication against the GBIF UserService.
   */
  @Override
  public Optional<UserPrincipal> authenticate(BasicCredentials credentials) throws AuthenticationException {
    User user = userService.authenticate(credentials.getUsername(), credentials.getPassword());
    if(user != null) { //User found
      LOG.debug("Authenticating user {} via scheme {}", credentials.getUsername(), SCHEME);
      return Optional.of(new UserPrincipal(user));
    }
    //User not found
    LOG.debug("User {} not found via scheme {}", credentials.getUsername(), SCHEME);
    return Optional.absent();
  }



}
