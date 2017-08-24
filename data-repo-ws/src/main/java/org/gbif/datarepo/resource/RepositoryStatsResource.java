package org.gbif.datarepo.resource;

import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.RepositoryStats;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.gbif.datarepo.resource.PathsParams.REPO_STATS_PATH;

/**
 * Resource to provide general stats about the repository.
 */
@Path(REPO_STATS_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryStatsResource {

  private final DataRepository dataRepository;

  /**
   * Default constructor.
   */
  public RepositoryStatsResource(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  /**
   * Serves the data repository statistics.
   */
  @GET
  public RepositoryStats get() {
    return dataRepository.getStats();
  }
}
