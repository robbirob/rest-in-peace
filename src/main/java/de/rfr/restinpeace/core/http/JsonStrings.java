package de.rfr.restinpeace.core.http;

/**
 * Small JSON string escaping helper used for framework-owned JSON error bodies.
 */
public final class JsonStrings {
    private JsonStrings() {
    }

    /**
     * Escapes a Java string for inclusion as a JSON string value.
     *
     * @param value raw value, may be {@code null}
     * @return escaped JSON string content without surrounding quotes
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            appendEscaped(builder, value.charAt(i));
        }
        return builder.toString();
    }

    private static void appendEscaped(StringBuilder builder, char value) {
        switch (value) {
            case '"' -> builder.append("\\\"");
            case '\\' -> builder.append("\\\\");
            case '\b' -> builder.append("\\b");
            case '\f' -> builder.append("\\f");
            case '\n' -> builder.append("\\n");
            case '\r' -> builder.append("\\r");
            case '\t' -> builder.append("\\t");
            default -> {
                if (value < 0x20) {
                    builder.append(String.format("\\u%04x", (int) value));
                } else {
                    builder.append(value);
                }
            }
        }
    }
}
