package de.rfr.restinpeace.core.error;

/**
 * Signals an unsupported request media type that maps to HTTP 415.
 */
public final class UnsupportedMediaTypeException extends RuntimeException {
    /**
     * Creates a new unsupported media type exception.
     *
     * @param message error message
     */
    public UnsupportedMediaTypeException(String message) {
        super(message);
    }
}
