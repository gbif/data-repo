package org.gbif.datarepo.identifiers.orcid;

import java.io.IOException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Simple Orcid client that validates the existence of a Orcid number.
 */
public class OrcidPublicClient implements AutoCloseable, OrcidPublicService {

  //Path to the profile web service
  private static final String ORCID_PROFILE_PATH = "orcid-profile";

  //Accepted response format
  public static final String APPLICATION_ORCID_JSON = "application/orcid+json";

  //Instance of jersey client
  private Client client;

  //Base web target that point to the doi registration url
  private final WebTarget webTarget;

  /**
   * Api type to contact.
   */
  public enum ApiType {

    LIVE("http://pub.orcid.org/v1.2"),
    SANDBOX("http://pub.sandbox.orcid.org/v1.2");

    //Url to Api Type
    private String apiUrl;

    ApiType(String apiUrl) {
      this.apiUrl = apiUrl;
    }
  }

  /**
   * Builds a Orcid client pointing to the Live api.
   */
  public OrcidPublicClient() {
    this(ApiType.LIVE);
  }

  /**
   * Builds a Orcid client pointing to the ApiType.url.
   */
  public OrcidPublicClient(ApiType apiType) {
    client = ClientBuilder.newClient();
    webTarget = client.target(apiType.apiUrl);
  }

  /**
   * Validates if the orcid parameter exists.
   */
  @Override
  public boolean exists(String orcid) {
    return Response.Status.OK.getStatusCode() ==
           webTarget.path(orcid).path(ORCID_PROFILE_PATH).request()
             .accept(APPLICATION_ORCID_JSON)
             .get().getStatus();
  }

  /**
   * Close the
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    client.close();
  }

}
