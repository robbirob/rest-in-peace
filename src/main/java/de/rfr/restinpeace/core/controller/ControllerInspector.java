package de.rfr.restinpeace.core.controller;

import de.rfr.restinpeace.api.annotations.Body;
import de.rfr.restinpeace.api.annotations.Consumes;
import de.rfr.restinpeace.api.annotations.DELETE;
import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.HeaderParam;
import de.rfr.restinpeace.api.annotations.POST;
import de.rfr.restinpeace.api.annotations.PUT;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.api.annotations.PathParam;
import de.rfr.restinpeace.api.annotations.Produces;
import de.rfr.restinpeace.api.annotations.QueryParam;
import de.rfr.restinpeace.core.http.HttpMethod;
import de.rfr.restinpeace.core.routing.Paths;
import de.rfr.restinpeace.core.routing.RouteDefinition;
import de.rfr.restinpeace.core.routing.RouteTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects controller instances at startup and compiles route definitions.
 */
public final class ControllerInspector {
    /**
     * Creates a new controller inspector.
     */
    public ControllerInspector() {
    }

    /**
     * Inspects all controllers and returns immutable route definitions.
     *
     * @param controllers controller instances
     * @return compiled route definitions
     * @throws IllegalArgumentException when controller metadata is invalid
     */
    public List<RouteDefinition> inspect(List<Object> controllers) {
        Objects.requireNonNull(controllers, "controllers must not be null");

        List<RouteDefinition> routes = new ArrayList<>();
        for (Object controller : controllers) {
            routes.addAll(inspectController(controller));
        }

        return List.copyOf(routes);
    }

    private List<RouteDefinition> inspectController(Object controller) {
        Objects.requireNonNull(controller, "controller must not be null");

        List<RouteDefinition> routes = new ArrayList<>();
        Class<?> controllerClass = controller.getClass();
        String classPath = resolveClassPath(controllerClass);

        for (Method method : controllerClass.getDeclaredMethods()) {
            HttpMethod httpMethod = resolveHttpMethod(method);
            if (httpMethod == null) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalArgumentException("handler method must be public. Make this method public: " + method);
            }

            List<ParameterDescriptor> parameters = resolveParameters(method);
            String methodPath = method.isAnnotationPresent(Path.class) ? method.getAnnotation(Path.class).value() : "";
            String fullPath = Paths.combine(classPath, methodPath);
            String consumes = method.isAnnotationPresent(Consumes.class) ? method.getAnnotation(Consumes.class).value() : null;
            String produces = method.isAnnotationPresent(Produces.class) ? method.getAnnotation(Produces.class).value() : null;

            HandlerMethod handlerMethod = new HandlerMethod(
                controller,
                method,
                parameters,
                consumes,
                produces
            );

            routes.add(new RouteDefinition(httpMethod, RouteTemplate.parse(fullPath), handlerMethod));
        }

        return routes;
    }

    private String resolveClassPath(Class<?> controllerClass) {
        Path classPath = controllerClass.getAnnotation(Path.class);
        if (classPath == null) {
            return "";
        }
        return classPath.value();
    }

    private HttpMethod resolveHttpMethod(Method method) {
        List<HttpMethod> annotations = new ArrayList<>(4);
        if (method.isAnnotationPresent(GET.class)) {
            annotations.add(HttpMethod.GET);
        }
        if (method.isAnnotationPresent(POST.class)) {
            annotations.add(HttpMethod.POST);
        }
        if (method.isAnnotationPresent(PUT.class)) {
            annotations.add(HttpMethod.PUT);
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            annotations.add(HttpMethod.DELETE);
        }

        if (annotations.isEmpty()) {
            return null;
        }
        if (annotations.size() > 1) {
            throw new IllegalArgumentException("handler must declare exactly one HTTP method annotation (@GET/@POST/@PUT/@DELETE): "
                + method);
        }

        return annotations.getFirst();
    }

    private List<ParameterDescriptor> resolveParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterDescriptor> descriptors = new ArrayList<>(parameters.length);
        int bodyParameters = 0;

        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            AnnotatedParameter annotated = resolveParameterKind(parameter);
            if (annotated.kind() == ParameterKind.BODY) {
                bodyParameters++;
            }
            if (bodyParameters > 1) {
                throw new IllegalArgumentException("handler can have at most one @Body parameter. Split body fields into one DTO: "
                    + method);
            }
            descriptors.add(new ParameterDescriptor(
                index,
                annotated.kind(),
                annotated.name(),
                parameter.getType(),
                parameter
            ));
        }

        return List.copyOf(descriptors);
    }

    private AnnotatedParameter resolveParameterKind(Parameter parameter) {
        List<Annotation> relevant = new ArrayList<>(4);
        PathParam pathParam = parameter.getAnnotation(PathParam.class);
        QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
        HeaderParam headerParam = parameter.getAnnotation(HeaderParam.class);
        Body body = parameter.getAnnotation(Body.class);

        if (pathParam != null) {
            relevant.add(pathParam);
        }
        if (queryParam != null) {
            relevant.add(queryParam);
        }
        if (headerParam != null) {
            relevant.add(headerParam);
        }
        if (body != null) {
            relevant.add(body);
        }

        if (relevant.size() > 1) {
            throw new IllegalArgumentException("parameter can only declare one binding annotation (@PathParam/@QueryParam/@HeaderParam/@Body): "
                + parameter);
        }

        if (pathParam != null) {
            requireNonBlank(pathParam.value(), "@PathParam", parameter);
            return new AnnotatedParameter(ParameterKind.PATH, pathParam.value());
        }
        if (queryParam != null) {
            requireNonBlank(queryParam.value(), "@QueryParam", parameter);
            return new AnnotatedParameter(ParameterKind.QUERY, queryParam.value());
        }
        if (headerParam != null) {
            requireNonBlank(headerParam.value(), "@HeaderParam", parameter);
            return new AnnotatedParameter(ParameterKind.HEADER, headerParam.value());
        }
        if (body != null) {
            return new AnnotatedParameter(ParameterKind.BODY, null);
        }

        throw new IllegalArgumentException("unannotated handler parameter is not supported. Add one of "
            + "@PathParam/@QueryParam/@HeaderParam/@Body: " + parameter);
    }

    private static void requireNonBlank(String value, String annotationName, Parameter parameter) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(annotationName + " value must not be blank: " + parameter);
        }
    }

    private record AnnotatedParameter(ParameterKind kind, String name) {
    }
}
