package org.gbif.datarepo.resource.caching;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class to purge the Varnish cache by sending a PURGE request to a path.
 */
public class Purger {

    private static final Logger LOG = LoggerFactory.getLogger(Purger.class);
    private final String DP_PATH = "data_packages/";
    private final CloseableHttpClient httpClient;
    private final String dataRepoApiUrl;

    /**
     * Full constructor.
     * @param apiUrl full url to the data repo url.
     */
    public Purger(String apiUrl) {
        httpClient = HttpClients.createDefault();
        this.dataRepoApiUrl = apiUrl + DP_PATH;
    }


    /**
     * Purge a single path.
     * @param path to purge
     */
    private void purgePath(String path) {
        try {
            String urlToBan = dataRepoApiUrl + path;
            LOG.debug("Purging url {}", urlToBan);
            HttpResponse response = httpClient.execute(RequestBuilder.create("BAN").setUri(dataRepoApiUrl)
                    .addHeader("X-Ban-URL", urlToBan).build());
            LOG.debug("BAN Response {}", response);
        } catch (IOException ex) {
            LOG.error("Error purging root path");
        }
    }

    /**
     * Purges a single resource and the root path that lists all resources.
     */
    public void purgeResource(String path) {
        purgeRoot();
        purgePath(path);
    }

    /**
     * Purges the relative root path.
     */
    public void purgeRoot() {
        try {
            LOG.debug("Purging url {}", dataRepoApiUrl);
            HttpResponse response = httpClient.execute(RequestBuilder.create("BAN").setUri(dataRepoApiUrl)
                    .addHeader("x-ban-url", dataRepoApiUrl).build());
            LOG.debug("BAN Response {}", response);
        } catch (IOException ex) {
            LOG.error("Error purging root path");
        }
    }

}
