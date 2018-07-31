package org.gbif.datarepo.resource.caching;

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
    private final String cacheUrl;

    /**
     * Full constructor.
     * @param cacheUrl full url to the data repo url.
     */
    public Purger(String cacheUrl) {
        httpClient = HttpClients.createDefault();
        this.cacheUrl = cacheUrl;
    }


    /**
     * Purge a single path.
     * @param path to purge
     */
    private void purgePath(String path) {
        try {
            String urlToBan = cacheUrl + DP_PATH + path;
            LOG.debug("Purging url {}", urlToBan);
            httpClient.execute(RequestBuilder.create("BAN").setUri(cacheUrl +  DP_PATH)
                    .addHeader("x-ban-url",urlToBan).build());
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
            String urlToBan = cacheUrl + DP_PATH;
            LOG.debug("Purging url {}", urlToBan);
            httpClient.execute(RequestBuilder.create("BAN").setUri(cacheUrl)
                    .addHeader("x-ban-url", urlToBan).build());
        } catch (IOException ex) {
            LOG.error("Error purging root path");
        }
    }

}
