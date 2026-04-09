package de.rfr.restinpeace.api.error;

import de.rfr.restinpeace.api.response.HttpResponse;

/**
 * Maps an exception type to an HTTP response.
 *
 * @param <E> mapped exception type
 * @since 0.1.0
 */
@FunctionalInterface
public interface ExceptionMapper<E extends Throwable> {
    /**
     * Converts an exception instance to an HTTP response.
     *
     * @param exception exception to map
     * @return mapped response
     */
    HttpResponse<?> map(E exception);
}
