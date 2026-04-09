package de.rfr.restinpeace.core.error;

/**
 * Signals that the request path exists but does not support the HTTP method.
 */
public final class MethodNotAllowedException extends RuntimeException {
    /**
     * Creates a new method-not-allowed exception.
     *
     * @param message error message
     */
    public MethodNotAllowedException(String message) {
        super(message);
    }
}
