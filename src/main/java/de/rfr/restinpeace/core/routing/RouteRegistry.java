package de.rfr.restinpeace.core.routing;

import de.rfr.restinpeace.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable route registry grouped by HTTP method and sorted by specificity.
 */
public final class RouteRegistry {
    private final Map<HttpMethod, List<RouteDefinition>> routesByMethod;

    /**
     * Creates a route registry and validates route conflicts.
     *
     * @param routes compiled routes
     * @throws IllegalArgumentException on ambiguous method/path patterns
     */
    public RouteRegistry(List<RouteDefinition> routes) {
        detectConflicts(routes);

        EnumMap<HttpMethod, List<RouteDefinition>> grouped = new EnumMap<>(HttpMethod.class);
        for (RouteDefinition route : routes) {
            grouped.computeIfAbsent(route.httpMethod(), ignored -> new ArrayList<>()).add(route);
        }

        for (List<RouteDefinition> definitions : grouped.values()) {
            definitions.sort(new RouteSpecificityComparator());
        }

        this.routesByMethod = Map.copyOf(grouped);
    }

    /**
     * Finds the best route match for method and path.
     *
     * @param method request method
     * @param path request path
     * @return optional route match
     */
    public Optional<RouteMatch> match(HttpMethod method, String path) {
        List<RouteDefinition> candidates = routesByMethod.getOrDefault(method, List.of());
        for (RouteDefinition candidate : candidates) {
            Map<String, String> pathParams = candidate.routeTemplate().match(path);
            if (pathParams != null) {
                return Optional.of(new RouteMatch(candidate, pathParams));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns number of registered routes.
     *
     * @return route count
     */
    public int size() {
        return routesByMethod.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Returns whether any route pattern matches the path regardless of method.
     *
     * @param path request path
     * @return {@code true} when at least one route pattern matches
     */
    public boolean hasAnyRouteForPath(String path) {
        for (List<RouteDefinition> definitions : routesByMethod.values()) {
            for (RouteDefinition definition : definitions) {
                if (definition.routeTemplate().match(path) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void detectConflicts(List<RouteDefinition> routes) {
        Map<String, RouteDefinition> seen = new HashMap<>();
        for (RouteDefinition route : routes) {
            String key = route.httpMethod() + " " + route.routeTemplate().canonicalPattern();
            RouteDefinition existing = seen.putIfAbsent(key, route);
            if (existing != null) {
                throw new IllegalArgumentException("ambiguous route definition: " + key
                    + " (first: " + existing.handlerMethod().method()
                    + ", duplicate: " + route.handlerMethod().method() + ")");
            }
        }
    }

    private static final class RouteSpecificityComparator implements Comparator<RouteDefinition> {
        @Override
        public int compare(RouteDefinition left, RouteDefinition right) {
            List<Integer> leftSpecificity = left.routeTemplate().specificityVector();
            List<Integer> rightSpecificity = right.routeTemplate().specificityVector();
            int length = Math.min(leftSpecificity.size(), rightSpecificity.size());

            for (int i = 0; i < length; i++) {
                int segmentDiff = Integer.compare(rightSpecificity.get(i), leftSpecificity.get(i));
                if (segmentDiff != 0) {
                    return segmentDiff;
                }
            }

            return Integer.compare(rightSpecificity.size(), leftSpecificity.size());
        }
    }
}
