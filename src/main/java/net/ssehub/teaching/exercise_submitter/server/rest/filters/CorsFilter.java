package net.ssehub.teaching.exercise_submitter.server.rest.filters;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.ext.Provider;

/**
 * A filter that adds the necessary headers for CORS to work properly.
 * <p>
 * See <a href="https://stackoverflow.com/a/28067653">https://stackoverflow.com/a/28067653</a>.
 * 
 * @author Adam
 */
@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    
    private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    
    private static final String REQUEST_METHOD = "Access-Control-Request-Method";
    
    private static final String REQUEST_HEADERS = "Access-Control-Request-Headers";
    
    private static final String VARY_HEADERS = "Origin, " + REQUEST_METHOD + ", " + REQUEST_HEADERS;
    
    
    /**
     * Handles pre-flight CORS requests by aborting with an OK response.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            getHeaderValue(requestContext, "Origin").ifPresent(origin -> {
                ResponseBuilder response = Response.noContent();
                
                response.header(ALLOW_ORIGIN, origin);
                response.header(ALLOW_CREDENTIALS, "true");
                getHeaderValue(requestContext, REQUEST_METHOD).ifPresent(
                        requestedMethod -> response.header(ALLOW_METHODS, requestedMethod));
                getHeaderValue(requestContext, REQUEST_HEADERS).ifPresent(
                        requestedHeaders -> response.header(ALLOW_HEADERS, requestedHeaders));
                response.header("Vary", VARY_HEADERS);
                
                requestContext.abortWith(response.build());
            });
        }
    }
    
    /**
     * Adds the required CORS headers to the response headers.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        
        if (!requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            getHeaderValue(requestContext, "Origin").ifPresent(origin -> {
                MultivaluedMap<String, Object> headers = responseContext.getHeaders();
                
                headers.putSingle(ALLOW_ORIGIN, origin);
                headers.putSingle(ALLOW_CREDENTIALS, "true");
                headers.putSingle(ALLOW_METHODS, requestContext.getMethod());
                headers.add("Vary", VARY_HEADERS);
            });
        }
        
    }
    
    /**
     * Convenience method to get the first header value from the given request.
     * 
     * @param requestContext The request to get the header value from.
     * @param header The name of the header to get.
     * 
     * @return The first value of the given header, or {@link Optional#empty()} if not specified.
     */
    private static Optional<String> getHeaderValue(ContainerRequestContext requestContext, String header) {
        List<String> headers = requestContext.getHeaders().get(header);
        Optional<String> result;
        if (headers != null && !headers.isEmpty()) {
            result = Optional.of(headers.get(0));
        } else {
            result = Optional.empty();
        }
        return result;
    }

}
