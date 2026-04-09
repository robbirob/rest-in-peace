package de.rfr.restinpeace.core.routing;

import de.rfr.restinpeace.core.controller.HandlerMethod;
import de.rfr.restinpeace.core.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteRegistryTest {
    @Test
    void exactRouteWinsOverVariableRoute() throws Exception {
        RouteDefinition exact = route("/users/me", "exact");
        RouteDefinition variable = route("/users/{id}", "variable");
        RouteRegistry registry = new RouteRegistry(List.of(variable, exact));

        RouteMatch match = registry.match(HttpMethod.GET, "/users/me").orElseThrow();

        assertEquals("/users/me", match.route().routeTemplate().path());
    }

    @Test
    void sameMethodAndCanonicalPatternIsRejected() throws Exception {
        RouteDefinition one = route("/users/{id}", "first");
        RouteDefinition two = route("/users/{name}", "second");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new RouteRegistry(List.of(one, two))
        );

        assertTrue(exception.getMessage().contains("ambiguous route definition"));
    }

    private static RouteDefinition route(String path, String methodName) throws Exception {
        Method method = TestController.class.getDeclaredMethod(methodName);
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method, List.of(), null, null);
        return new RouteDefinition(HttpMethod.GET, RouteTemplate.parse(path), handlerMethod);
    }

    private static final class TestController {
        public String exact() {
            return "ok";
        }

        public String variable() {
            return "ok";
        }

        public String first() {
            return "ok";
        }

        public String second() {
            return "ok";
        }
    }
}
