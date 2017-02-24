package org.gbif.datarepo.test.utils;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.conf.DataRepoConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.basic.BasicCredentials;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * Utility class containing methods to reduce complexity in other classes.
 */
public class ResourceTestUtils {

  public static final String TEST_REPO_PATH = "src/test/resources/testrepo/";

  public static final String TEST_DATA_PACKAGE_DIR = TEST_REPO_PATH + DOI.TEST_PREFIX +  "-dp.bvmv02/";

  public static final String CONTENT_TEST_FILE = "occurrence.txt";

  public static final String METADATA_TEST_FILE = "metadata.xml";

  public static final UserPrincipal TEST_USER = testUser();

  public static final String TEST_USER_CREDENTIALS = encodedTestUserCredentials();

  public static final BasicCredentials TEST_BASIC_CREDENTIALS = basicCredentials();

  private static final String TEST_USER_NAME = "test";

  /**
   * Private constructor.
   */
  private ResourceTestUtils() {
    //empty constructor
  }


  /**
   * Creates a test user instance.
   * User name and password are identical.
   */
  private static UserPrincipal testUser() {
    User user = new User();
    user.setUserName(TEST_USER_NAME);
    user.setPasswordHash(TEST_USER_NAME);
    return new UserPrincipal(user);
  }

  /**
   * Creates a Base64 encoded user credentials.
   */
  private static String encodedTestUserCredentials() {
    return "Basic " + new String(Base64.encode((TEST_USER_NAME + ':' + TEST_USER_NAME).getBytes()));
  }

  /**
   * Creates a test instance from BasicCredentials using the test user data.
   */
  private static BasicCredentials basicCredentials() {
    return new BasicCredentials(TEST_USER.getUser().getUserName(), TEST_USER.getUser().getPasswordHash());
  }

  public static FormDataBodyPart dataBodyPartOf(String testFile, String formParam) throws IOException {
    String testFileName = TEST_DATA_PACKAGE_DIR + testFile;
    return new FormDataBodyPart(FormDataContentDisposition.name(formParam).fileName(testFileName).build(),
                                new FileInputStream(testFileName), MediaType.APPLICATION_OCTET_STREAM_TYPE);
  }

  /**
   * Gets DataPackage instance using a file system directory as the source for information.
   */
  public static Optional<DataPackage> getFromLocalFileSystem(DOI doi, DataRepoConfiguration configuration) {
    File doiPath = Paths.get(configuration.getDataRepoPath())
                    .resolve(doi.getPrefix() + '-' + doi.getSuffix()).toFile();
    if (doiPath.exists()) {
      //Assemble a new DataPackage instance containing all the information
      DataPackage dataPackage = new DataPackage();
      dataPackage.setDoi(doi);
      dataPackage.setMetadata(DataPackage.METADATA_FILE);
      Arrays.stream(doiPath.listFiles(pathname -> !pathname.getName().equals(DataPackage.METADATA_FILE)))
        .forEach(file -> dataPackage.addFile(file.getName())); //metadata.xml is excluded from the list from files
      return Optional.of(dataPackage);
    }
    return Optional.empty();
  }

}
