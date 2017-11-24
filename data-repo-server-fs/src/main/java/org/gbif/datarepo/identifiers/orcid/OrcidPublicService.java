package org.gbif.datarepo.identifiers.orcid;

/**
 * Simple interface to validate existence of orcids.
 */
public interface OrcidPublicService {

  /**
   * Does the orcid exists?.
   */
  boolean exists(String orcid);
}
