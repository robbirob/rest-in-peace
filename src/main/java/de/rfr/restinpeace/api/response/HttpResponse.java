package de.rfr.restinpeace.api.response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable HTTP response abstraction returned by handlers and exception mappers.
 *
 * @param <T> response body type
 * @since 0.1.0
 */
public final class HttpResponse<T> {
    private final int status;
    private final Map<String, String> headers;
    private final T body;

    private HttpResponse(int status, Map<String, String> headers, T body) {
        this.status = status;
        this.headers = Map.copyOf(headers);
        this.body = body;
    }

    /**
     * Creates a {@code 200 OK} response.
     *
     * @param body response body
     * @param <T> body type
     * @return new response with status 200
     */
    public static <T> HttpResponse<T> ok(T body) {
        return status(200, body);
    }

    /**
     * Creates a {@code 201 Created} response.
     *
     * @param body response body
     * @param <T> body type
     * @return new response with status 201
     */
    public static <T> HttpResponse<T> created(T body) {
        return status(201, body);
    }

    /**
     * Creates a {@code 204 No Content} response.
     *
     * @return new response with status 204 and no body
     */
    public static HttpResponse<Void> noContent() {
        return new HttpResponse<>(204, Map.of(), null);
    }

    /**
     * Creates a response with the provided HTTP status code.
     *
     * @param status HTTP status code (100-599)
     * @param body response body
     * @param <T> body type
     * @return new response with the provided status and body
     * @throws IllegalArgumentException when status is outside the HTTP range
     */
    public static <T> HttpResponse<T> status(int status, T body) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be between 100 and 599");
        }
        return new HttpResponse<>(status, Map.of(), body);
    }

    /**
     * Returns a copy of this response with an additional or replaced header.
     *
     * @param name header name
     * @param value header value
     * @return copied response with the updated header map
     */
    public HttpResponse<T> header(String name, String value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        LinkedHashMap<String, String> nextHeaders = new LinkedHashMap<>(headers);
        nextHeaders.put(name, value);
        return new HttpResponse<>(status, nextHeaders, body);
    }

    /**
     * Returns the HTTP status code.
     *
     * @return status code
     */
    public int status() {
        return status;
    }

    /**
     * Returns immutable response headers.
     *
     * @return response header map
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Returns the response body.
     *
     * @return response body, may be {@code null}
     */
    public T body() {
        return body;
    }
}
