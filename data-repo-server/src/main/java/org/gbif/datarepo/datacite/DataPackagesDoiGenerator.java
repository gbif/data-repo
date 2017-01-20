package org.gbif.datarepo.datacite;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.datacite.DataCiteService;

import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class DataPackagesDoiGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DataPackagesDoiGenerator.class);
  private static final String DP_SHOULDER = "DP";
  private static final int RANDOM_LENGTH = 6;

  private final DataCiteService dataCiteService;
  private final String prefix;

  public DataPackagesDoiGenerator(String prefix, DataCiteService dataCiteService) {
    checkArgument(prefix.startsWith("10."), "DOI prefix must begin with 10.");
    this.prefix = prefix;
    this.dataCiteService = dataCiteService;
  }

  /**
   * @return a random DOI with the given prefix. It is not guaranteed to be unique and might exist already
   */
  private DOI randomDOI() {
    String suffix = Strings.nullToEmpty(DP_SHOULDER) + RandomStringUtils.randomAlphanumeric(RANDOM_LENGTH);
    return new DOI(prefix, suffix);
  }


  public DOI newDOI() {
    // only try for hundred times then fail
    for (int x=0; x<100; x++) {
      DOI doi = randomDOI();
      try {
        if(!dataCiteService.exists(doi)) {
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
