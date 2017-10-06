package org.gbif.datarepo.test.resource;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.GbifUserPrincipal;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.datarepo.auth.jwt.JwtAuthenticator;
import org.gbif.datarepo.auth.jwt.JwtAuthConfiguration;
import org.gbif.datarepo.auth.jwt.JwtCredentialsFilter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthTest {

  static final String TEST_RESOURCE_PATH = "test";
  static final String AUTHORIZED_USER_NAME = "Testatarian";
  static final String NOTAUTHORIZED_USER_NAME = "Cheater";

  @Produces(MediaType.APPLICATION_JSON)
  @Path(TEST_RESOURCE_PATH)
  public static class TestResource {

    @GET
    public Response secure(@Auth GbifUserPrincipal principal) {
      return Response.ok().build();
    }

  }

  //Grizzly is required since he in-memory Jersey test container does not support all features,
  // such as the @Context injection used by BasicAuthFactory and OAuthFactory.
  @ClassRule
  public static ResourceTestRule resource = ResourceTestRule.builder()
    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
    .addProvider(new AuthDynamicFeature(jwtAuthFilter()))
    .addProvider(new AuthValueFactoryProvider.Binder<>(GbifUserPrincipal.class))
    .addResource(new TestResource())
    .build();

  //Key used to sign tokens
  private static final String JWT_SIGNING_KEY = "ABCD";

  //Bad key used to check unauthorized access
  private static final String JWT_BAD_SIGNING_KEY = "EFGH";

  /**
   * Creates JWT token containing the data: {userName: userNameArgument}.
   * @param userName test user name
   * @param signingKey jwt signing key
   * @return a JWT signed token
   */
  private static String getJwtToken(String userName, byte[] signingKey) {
    return Jwts.builder()
      .signWith(SignatureAlgorithm.HS256, signingKey)
      .claim(JwtAuthConfiguration.DEFAULT_JWT_USER_NAME, userName)
      .compact();
  }

  /**
   * Generates a test user.
   */
  private static GbifUser testUser() {
    GbifUser gbifUser = new GbifUser();
    gbifUser.setUserName(AUTHORIZED_USER_NAME);
    return gbifUser;
  }

  /**
   * Builds a GbifJwtCredentialsFilter that returns testUser() for all authenticated users.
   */
  private static AuthFilter<String, GbifUserPrincipal> jwtAuthFilter() {
    JwtAuthConfiguration configuration = new JwtAuthConfiguration();
    configuration.setSigningKey(JWT_SIGNING_KEY);
    IdentityAccessService identityAccessService = Mockito.mock(IdentityAccessService.class);
    Mockito.when(identityAccessService.get(Matchers.matches(AUTHORIZED_USER_NAME))).thenReturn(testUser());
    return new JwtCredentialsFilter.Builder()
      .setAuthenticator(new JwtAuthenticator(configuration, identityAccessService))
      .buildAuthFilter();
  }

  /**
   * Base method to run JWT auth test using a userName, a jwtSigningKey and a expected result.
   * This test adds a cookie with the name GbifJwtCredentialsFilter.SECURITY_COOKIE containing the JWT token data.
   */
  private static void secureResourceTest(String userName, String jwtSigningKey, Response.StatusType expectedStatus) {
    assertThat(resource.getJerseyTest()
                 .target(TEST_RESOURCE_PATH)
                 .request()
                 .cookie(JwtAuthConfiguration.DEFAULT_SECURITY_COOKIE,
                         getJwtToken(userName, jwtSigningKey.getBytes()))
                 .get(Response.class).getStatus())
      .isEqualTo(expectedStatus.getStatusCode());
  }

  /**
   * Tests that resource can be called using an authorized user name.
   */
  @Test
  public void testAuthorizedToken() {
    secureResourceTest(AUTHORIZED_USER_NAME, JWT_SIGNING_KEY, Response.Status.OK);
  }

  /**
   * Tests that resource can NOT be called using an unauthorized user name.
   */
  @Test
  public void testBadSigningKey() {
    secureResourceTest(AUTHORIZED_USER_NAME, JWT_BAD_SIGNING_KEY, Response.Status.UNAUTHORIZED);
  }

  /**
   * Tests with a valid signing key but using an NON-existent user.
   */
  @Test
  public void testNotAuthorizedToken() {
    secureResourceTest(NOTAUTHORIZED_USER_NAME, JWT_BAD_SIGNING_KEY, Response.Status.UNAUTHORIZED);
  }

}
