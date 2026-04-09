package de.rfr.restinpeace.core.routing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed route template supporting literal and variable path segments.
 */
public final class RouteTemplate {
    private final String path;
    private final List<Segment> segments;

    private RouteTemplate(String path, List<Segment> segments) {
        this.path = path;
        this.segments = List.copyOf(segments);
    }

    /**
     * Parses a route template from a raw path string.
     *
     * @param path route path
     * @return parsed template
     */
    public static RouteTemplate parse(String path) {
        String normalized = Paths.normalize(path);
        if ("/".equals(normalized)) {
            return new RouteTemplate(normalized, List.of());
        }

        String[] rawSegments = normalized.substring(1).split("/");
        List<Segment> segments = new ArrayList<>(rawSegments.length);
        for (String segment : rawSegments) {
            segments.add(parseSegment(segment));
        }

        return new RouteTemplate(normalized, segments);
    }

    /**
     * Returns the normalized route path used for this template.
     *
     * @return normalized route path
     */
    public String path() {
        return path;
    }

    /**
     * Returns number of path segments in this template.
     *
     * @return segment count
     */
    public int segmentCount() {
        return segments.size();
    }

    /**
     * Returns whether a segment at index is a variable segment.
     *
     * @param index segment index
     * @return {@code true} when segment is variable
     */
    public boolean isVariableSegment(int index) {
        return segments.get(index) instanceof VariableSegment;
    }

    /**
     * Returns the variable name at the given segment index.
     *
     * @param index segment index
     * @return variable segment name
     * @throws IllegalArgumentException when segment is literal
     */
    public String variableName(int index) {
        Segment segment = segments.get(index);
        if (segment instanceof VariableSegment variable) {
            return variable.name();
        }
        throw new IllegalArgumentException("segment is not a variable segment");
    }

    /**
     * Returns canonical pattern where all variable names are normalized to {@code {}}.
     *
     * @return canonical pattern string
     */
    public String canonicalPattern() {
        if (segments.isEmpty()) {
            return "/";
        }
        List<String> parts = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            parts.add(segment.canonicalPart());
        }
        return "/" + String.join("/", parts);
    }

    /**
     * Returns a segment-by-segment specificity vector used for route sorting.
     *
     * @return immutable specificity vector
     */
    public List<Integer> specificityVector() {
        List<Integer> specificity = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            specificity.add(segment.specificity());
        }
        return List.copyOf(specificity);
    }

    /**
     * Attempts to match a request path and extract path variables.
     *
     * @param requestPath request path
     * @return immutable extracted path params or {@code null} when not matched
     */
    public Map<String, String> match(String requestPath) {
        String normalizedRequestPath = Paths.normalize(requestPath);
        if ("/".equals(normalizedRequestPath)) {
            return segments.isEmpty() ? Map.of() : null;
        }

        String[] requestSegments = normalizedRequestPath.substring(1).split("/");
        if (requestSegments.length != segments.size()) {
            return null;
        }

        LinkedHashMap<String, String> pathParams = new LinkedHashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment templateSegment = segments.get(i);
            String requestSegment = requestSegments[i];
            if (!templateSegment.matches(requestSegment, pathParams)) {
                return null;
            }
        }
        return Map.copyOf(pathParams);
    }

    private static Segment parseSegment(String value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.startsWith("{") && value.endsWith("}")) {
            String name = value.substring(1, value.length() - 1).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("path variable name must not be empty");
            }
            return new VariableSegment(name);
        }
        return new LiteralSegment(value);
    }

    private sealed interface Segment permits LiteralSegment, VariableSegment {
        String canonicalPart();

        int specificity();

        boolean matches(String requestSegment, Map<String, String> pathParams);
    }

    private record LiteralSegment(String value) implements Segment {
        @Override
        public String canonicalPart() {
            return value;
        }

        @Override
        public int specificity() {
            return 1;
        }

        @Override
        public boolean matches(String requestSegment, Map<String, String> pathParams) {
            return value.equals(requestSegment);
        }
    }

    private record VariableSegment(String name) implements Segment {
        @Override
        public String canonicalPart() {
            return "{}";
        }

        @Override
        public int specificity() {
            return 0;
        }

        @Override
        public boolean matches(String requestSegment, Map<String, String> pathParams) {
            pathParams.put(name, requestSegment);
            return true;
        }
    }
}
