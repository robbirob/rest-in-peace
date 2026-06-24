package de.rfr.restinpeace.core.http;

import java.util.Locale;

/**
 * Media type parsing helpers for HTTP Content-Type values.
 */
public final class MediaTypes {
    private MediaTypes() {
    }

    /**
     * Returns whether a Content-Type header exactly matches an expected media type.
     * Parameters are ignored and comparison is case-insensitive.
     *
     * @param actual actual Content-Type header value
     * @param expected expected media type without parameters
     * @return {@code true} when media types match
     */
    public static boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return mediaType(actual).equals(mediaType(expected));
    }

    /**
     * Returns whether a Content-Type represents JSON, including structured +json types.
     *
     * @param contentType Content-Type header value
     * @return {@code true} for application/json or application/*+json media types
     */
    public static boolean isJson(String contentType) {
        String mediaType = mediaType(contentType);
        return "application/json".equals(mediaType)
            || (mediaType.startsWith("application/") && mediaType.endsWith("+json"));
    }

    private static String mediaType(String contentType) {
        if (contentType == null) {
            return "";
        }

        int parameterStart = contentType.indexOf(';');
        String value = parameterStart >= 0 ? contentType.substring(0, parameterStart) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
