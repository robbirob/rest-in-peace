package de.rfr.restinpeace.core.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Internal normalized HTTP request abstraction used by the invocation pipeline.
 */
public interface FrameworkRequest {
    /**
     * Returns the HTTP method.
     *
     * @return HTTP method
     */
    HttpMethod method();

    /**
     * Returns the normalized request path.
     *
     * @return request path
     */
    String path();

    /**
     * Returns query parameters as multimap values.
     *
     * @return immutable query parameter map
     */
    Map<String, List<String>> queryParams();

    /**
     * Returns request headers as multimap values.
     *
     * @return immutable header map
     */
    Map<String, List<String>> headers();

    /**
     * Returns the body input stream.
     *
     * @return request body stream
     */
    InputStream body();

    /**
     * Returns the request content type header value.
     *
     * @return content type or {@code null}
     */
    String contentType();
}
