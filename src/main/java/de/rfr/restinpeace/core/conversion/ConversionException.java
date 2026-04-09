package de.rfr.restinpeace.core.conversion;

/**
 * Signals that a scalar conversion failed.
 */
public final class ConversionException extends RuntimeException {
    /**
     * Creates a new conversion exception with message and root cause.
     *
     * @param message error message
     * @param cause root cause
     */
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new conversion exception with message only.
     *
     * @param message error message
     */
    public ConversionException(String message) {
        super(message);
    }
}
