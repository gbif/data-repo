package org.gbif.datarepo.health;

import org.gbif.api.model.common.GbifUserPrincipal;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * HealthCheck to validate that the user service is up and running.
 */
public class AuthenticatorHealthCheck extends HealthCheck {

  private final Authenticator<BasicCredentials, GbifUserPrincipal> authenticator;
  private static final int CREDENTIALS_LENGTH = 5;

  /**
   * @param authenticator to be contacted
   */
  public AuthenticatorHealthCheck(Authenticator<BasicCredentials, GbifUserPrincipal> authenticator) {
    this.authenticator = authenticator;
  }

  /**
   * Generates a random user name and tries to get its data.
   * The result is ignore, this method only tests that the UserService is operational.
   */
  @Override
  protected Result check() throws Exception {
    String userAndPassword = RandomStringUtils.random(CREDENTIALS_LENGTH);
    authenticator.authenticate(new BasicCredentials(userAndPassword, userAndPassword));
    return Result.healthy("User authentication service it's running");
  }
}
