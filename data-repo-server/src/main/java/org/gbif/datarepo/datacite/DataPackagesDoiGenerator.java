package org.gbif.datarepo.datacite;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.datacite.DataCiteService;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class to generate DOIs for data packages.
 */
public class DataPackagesDoiGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DataPackagesDoiGenerator.class);

  //Length of random part of generated DOIs
  private static final int RANDOM_LENGTH = 6;

  //DataCite service instance, used to validate DOIs existence
  private final DataCiteService dataCiteService;

  //DOI common prefix
  private final String prefix;

  //Invariant section of a DOI
  private final String shoulder;

  public DataPackagesDoiGenerator(String prefix, String shoulder, DataCiteService dataCiteService) {
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with 10.");
    checkNotNull(shoulder, "Shoulder parameter can't be null");
    this.prefix = prefix;
    this.shoulder = shoulder;
    this.dataCiteService = dataCiteService;
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist already
   */
  public DOI randomDOI() {
    return new DOI(prefix, shoulder + RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH));
  }

  /**
   * Generates a new DOI that doesn't exist at DataCite at the moment of invocation.
   */
  public DOI newDOI() {
    // only try for hundred times then fail
    for (int x=0; x < 100; x++) {
      DOI doi = randomDOI();
      try {
        if(!dataCiteService.exists(doi)) { //exists in DataCite?
          return doi;
        }
      } catch (DoiException e) {
        // might have hit a unique constraint, try another doi
        LOG.debug("Random {} DOI {} existed. Try another one", doi, e.getMessage());
      }
    }
    throw new IllegalStateException("Tried 100 random DOIs and none worked, Giving up");
  }
}
