package de.rfr.restinpeace.core;

import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.core.invocation.RequestExecutor;
import de.rfr.restinpeace.core.routing.RouteRegistry;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Immutable runtime configuration assembled by {@link de.rfr.restinpeace.RestApp.Builder}.
 *
 * @param controllers registered controller instances
 * @param codecs registered body codecs
 * @param exceptionMappers registered custom exception mappers
 * @param executor optional transport executor
 * @param routeRegistry compiled route registry
 * @param requestExecutor request execution pipeline
 */
public record AppConfig(
    List<Object> controllers,
    List<BodyCodec> codecs,
    List<RestApp.ExceptionMapperRegistration<?>> exceptionMappers,
    Executor executor,
    RouteRegistry routeRegistry,
    RequestExecutor requestExecutor
) {
}
