package de.rfr.restinpeace.core.invocation;

import de.rfr.restinpeace.api.response.HttpResponse;
import de.rfr.restinpeace.core.binding.ParameterBinder;
import de.rfr.restinpeace.core.controller.HandlerMethod;
import de.rfr.restinpeace.core.controller.ParameterKind;
import de.rfr.restinpeace.core.error.BadRequestException;
import de.rfr.restinpeace.core.error.ExceptionMapperRegistry;
import de.rfr.restinpeace.core.error.MethodNotAllowedException;
import de.rfr.restinpeace.core.error.UnsupportedMediaTypeException;
import de.rfr.restinpeace.core.http.FrameworkError;
import de.rfr.restinpeace.core.http.FrameworkRequest;
import de.rfr.restinpeace.core.http.FrameworkResponse;
import de.rfr.restinpeace.core.http.JsonStrings;
import de.rfr.restinpeace.core.http.MediaTypes;
import de.rfr.restinpeace.core.routing.RouteMatch;
import de.rfr.restinpeace.core.routing.RouteRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * End-to-end request execution pipeline from route matching to response mapping.
 */
public final class RequestExecutor {
    private final RouteRegistry routeRegistry;
    private final ParameterBinder parameterBinder;
    private final ExceptionMapperRegistry exceptionMappers;

    /**
     * Creates a request executor.
     *
     * @param routeRegistry route registry
     * @param parameterBinder parameter binder
     * @param exceptionMappers custom exception mapper registry
     */
    public RequestExecutor(
        RouteRegistry routeRegistry,
        ParameterBinder parameterBinder,
        ExceptionMapperRegistry exceptionMappers
    ) {
        this.routeRegistry = routeRegistry;
        this.parameterBinder = parameterBinder;
        this.exceptionMappers = exceptionMappers;
    }

    /**
     * Executes a framework request and returns a framework response.
     *
     * @param request incoming request abstraction
     * @return normalized response
     */
    public FrameworkResponse execute(FrameworkRequest request) {
        try {
            return executeInternal(request);
        } catch (Exception exception) {
            return toFrameworkResponse(resolveException(unwrap(exception)));
        }
    }

    private FrameworkResponse executeInternal(FrameworkRequest request) throws Exception {
        RouteMatch routeMatch = routeRegistry.match(request.method(), request.path()).orElse(null);
        if (routeMatch == null) {
            if (routeRegistry.hasAnyRouteForPath(request.path())) {
                throw new MethodNotAllowedException("method not allowed");
            }
            return toFrameworkResponse(HttpResponse.status(404, new FrameworkError("not_found", "no route matched")));
        }

        HandlerMethod handler = routeMatch.route().handlerMethod();
        enforceConsumes(handler, request.contentType());
        Object[] args = parameterBinder.bind(handler, request, routeMatch.pathParams());
        Method method = handler.method();
        Object result = method.invoke(handler.controller(), args);
        return toFrameworkResponse(applyProduces(handler, mapResult(method, result)));
    }

    private HttpResponse<?> mapResult(Method method, Object result) {
        if (result instanceof HttpResponse<?> explicitResponse) {
            return explicitResponse;
        }
        if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
            return HttpResponse.noContent();
        }
        return HttpResponse.ok(result);
    }

    private HttpResponse<?> resolveException(Throwable throwable) {
        try {
            return exceptionMappers.map(throwable)
                .orElseGet(() -> defaultExceptionResponse(throwable));
        } catch (Exception mapperException) {
            return defaultExceptionResponse(mapperException);
        }
    }

    private HttpResponse<?> defaultExceptionResponse(Throwable throwable) {
        if (throwable instanceof MethodNotAllowedException) {
            return HttpResponse.status(405, new FrameworkError("method_not_allowed", throwable.getMessage()));
        }
        if (throwable instanceof BadRequestException) {
            return HttpResponse.status(400, new FrameworkError("bad_request", throwable.getMessage()));
        }
        if (throwable instanceof UnsupportedMediaTypeException) {
            return HttpResponse.status(415, new FrameworkError("unsupported_media_type", throwable.getMessage()));
        }
        return HttpResponse.status(500, new FrameworkError("internal_server_error", "unhandled exception"));
    }

    private static void enforceConsumes(HandlerMethod handler, String requestContentType) {
        if (!hasBodyParameter(handler)) {
            return;
        }

        String consumes = handler.consumes();
        if (consumes == null || consumes.isBlank()) {
            return;
        }

        if (requestContentType == null || requestContentType.isBlank()) {
            throw new UnsupportedMediaTypeException("missing Content-Type, expected " + consumes);
        }

        if (!MediaTypes.matches(requestContentType, consumes)) {
            throw new UnsupportedMediaTypeException("unsupported Content-Type: " + requestContentType);
        }
    }

    private static HttpResponse<?> applyProduces(HandlerMethod handler, HttpResponse<?> response) {
        String produces = handler.produces();
        if (produces == null || produces.isBlank()) {
            return response;
        }

        if (hasHeader(response.headers(), "Content-Type")) {
            return response;
        }
        return response.header("Content-Type", produces);
    }

    private static boolean hasBodyParameter(HandlerMethod handler) {
        return handler.parameters().stream().anyMatch(parameter -> parameter.kind() == ParameterKind.BODY);
    }

    private static boolean hasHeader(Map<String, String> headers, String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException
            && invocationTargetException.getTargetException() != null) {
            return invocationTargetException.getTargetException();
        }
        return throwable;
    }

    private static FrameworkResponse toFrameworkResponse(HttpResponse<?> response) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>(response.headers());
        Object body = response.body();
        if (body instanceof FrameworkError error) {
            if (!hasHeader(headers, "Content-Type")) {
                headers.put("Content-Type", "application/json");
            }
            body = frameworkErrorJson(error);
        }
        return new FrameworkResponse(response.status(), Map.copyOf(headers), body);
    }

    private static String frameworkErrorJson(FrameworkError error) {
        return "{\"code\":\"" + JsonStrings.escape(error.code()) + "\",\"message\":\""
            + JsonStrings.escape(error.message()) + "\"}";
    }
}
