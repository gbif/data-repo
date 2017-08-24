package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * DOI Mock RegistrationService. Used for unit tests only.
 */
public class DoiRegistrationServiceMock implements DoiRegistrationService {

  private static final int RANDOM_LENGTH = 6;

  /**
   * Creates a random DOI.
   */
  @Override
  public DOI generate(DoiType doiType) {
    return randomDOI();
  }

  /**
   * Creates a DOI instance from the prefix and suffix parameters.
   */
  @Override
  public DoiData get(String prefix, String suffix) {
   return new DoiData(DoiStatus.REGISTERED, new DOI(prefix, suffix).getUrl());
  }

  /**
   * Returns a random DOI if the doiRegistration parameter doesn't have a DOI, returns doiRegistration.doi otherwise.
   */
  @Override
  public DOI register(DoiRegistration doiRegistration) {
    if (doiRegistration.getDoi() == null) {
      return randomDOI();
    }
    return doiRegistration.getDoi();
  }

  /**
   * Returns a random DOI if the doiRegistration parameter doesn't have a DOI, returns doiRegistration.doi otherwise.
   */
  @Override
  public DOI update(DoiRegistration doiRegistration) {
    return doiRegistration.getDoi();
  }

  /**
   * Does not perform any operation.
   */
  @Override
  public void delete(String prefix, String suffix) {
    // NOP
  }

  /**
   * Generates random DataPackage DOI.
   */
  private static  DOI randomDOI() {
    return new DOI(DOI.TEST_PREFIX, "dp." + RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH));
  }
}
