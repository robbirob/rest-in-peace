package de.rfr.restinpeace.core.http;

import java.util.Map;

/**
 * Internal normalized response model produced by the invocation pipeline.
 *
 * @param status HTTP status code
 * @param headers immutable response header map
 * @param body response body object
 */
public record FrameworkResponse(
    int status,
    Map<String, String> headers,
    Object body
) {
}
