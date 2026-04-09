package de.rfr.restinpeace.core.routing;

import de.rfr.restinpeace.core.controller.HandlerMethod;
import de.rfr.restinpeace.core.http.HttpMethod;

/**
 * A compiled route mapping from HTTP method and path template to a handler.
 *
 * @param httpMethod HTTP method
 * @param routeTemplate parsed route template
 * @param handlerMethod compiled handler metadata
 */
public record RouteDefinition(
    HttpMethod httpMethod,
    RouteTemplate routeTemplate,
    HandlerMethod handlerMethod
) {
}
