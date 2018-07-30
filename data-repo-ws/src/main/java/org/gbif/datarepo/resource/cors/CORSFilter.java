package org.gbif.datarepo.resource.cors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple Cross-Origin Resource Sharing filter.
 */
public class CORSFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CORSFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //DO NOTHING
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LOG.info("Filtering request {}", request);
        HttpServletResponse httpServletResponse = (HttpServletResponse)response;
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");

        httpServletResponse.addHeader("Access-Control-Allow-Methods", "HEAD, GET, POST, DELETE, PUT, OPTIONS");

        //Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
        //https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Preflighted_requests
        //we reflect the headers specified in the Access-Control-Request-Headers header of the request
        if(httpServletRequest.getHeader("Access-Control-Request-Headers") != null){
            httpServletResponse.addHeader("Access-Control-Allow-Headers",
                    httpServletRequest.getHeader("Access-Control-Request-Headers"));
        }
    }

    @Override
    public void destroy() {
        //DO Nothing
    }
}
