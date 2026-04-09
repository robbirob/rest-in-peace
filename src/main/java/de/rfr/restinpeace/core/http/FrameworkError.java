package de.rfr.restinpeace.core.http;

/**
 * Structured framework error payload used for JSON error responses.
 *
 * @param code stable machine-readable error code
 * @param message human-readable error message
 */
public record FrameworkError(String code, String message) {
}
