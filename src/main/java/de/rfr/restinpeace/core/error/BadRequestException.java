package de.rfr.restinpeace.core.error;

/**
 * Signals an invalid client request that maps to HTTP 400.
 */
public final class BadRequestException extends RuntimeException {
    /**
     * Creates a new bad request exception.
     *
     * @param message error message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Creates a new bad request exception with root cause.
     *
     * @param message error message
     * @param cause root cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
