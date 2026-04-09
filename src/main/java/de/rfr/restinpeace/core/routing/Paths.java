package de.rfr.restinpeace.core.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utilities for path normalization and class/method path composition.
 */
public final class Paths {
    private Paths() {
    }

    /**
     * Normalizes a path by ensuring a leading slash and collapsing empty segments.
     *
     * @param path raw path
     * @return normalized absolute path
     */
    public static String normalize(String path) {
        Objects.requireNonNull(path, "path must not be null");

        if (path.isBlank() || "/".equals(path)) {
            return "/";
        }

        String working = path.startsWith("/") ? path : "/" + path;
        String[] rawSegments = working.split("/");
        List<String> segments = new ArrayList<>();
        for (String segment : rawSegments) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }

        if (segments.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", segments);
    }

    /**
     * Combines class-level and method-level path fragments into one normalized path.
     *
     * @param classPath class-level path fragment
     * @param methodPath method-level path fragment
     * @return normalized combined path
     */
    public static String combine(String classPath, String methodPath) {
        String left = classPath == null ? "" : classPath;
        String right = methodPath == null ? "" : methodPath;
        return normalize(left + "/" + right);
    }
}
