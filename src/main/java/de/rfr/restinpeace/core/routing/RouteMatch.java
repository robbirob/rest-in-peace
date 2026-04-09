package de.rfr.restinpeace.core.routing;

import java.util.Map;

/**
 * Successful route lookup result with extracted path parameters.
 *
 * @param route matched route definition
 * @param pathParams resolved path parameter values
 */
public record RouteMatch(
    RouteDefinition route,
    Map<String, String> pathParams
) {
}
