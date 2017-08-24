package org.gbif.datarepo.persistence.mappers;

import org.gbif.datarepo.api.model.RepositoryStats;

/**
 * MyBatis mapper that provides information about the data repository.
 */
public interface RepositoryStatsMapper {

  /**
   * Retrieves the RepositoryStats of active files.
   */
  RepositoryStats get();
}
