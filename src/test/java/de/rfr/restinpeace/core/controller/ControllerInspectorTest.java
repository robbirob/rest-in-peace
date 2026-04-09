package de.rfr.restinpeace.core.controller;

import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.core.routing.RouteDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControllerInspectorTest {
    @Test
    void composesClassAndMethodPaths() {
        ControllerInspector inspector = new ControllerInspector();

        List<RouteDefinition> routes = inspector.inspect(List.of(new UsersController()));

        assertEquals(1, routes.size());
        assertEquals("/users/{id}", routes.getFirst().routeTemplate().path());
    }

    @Test
    void rejectsUnannotatedHandlerParameter() {
        ControllerInspector inspector = new ControllerInspector();

        assertThrows(IllegalArgumentException.class, () -> inspector.inspect(List.of(new InvalidController())));
    }

    @Path("/users/")
    private static final class UsersController {
        @GET
        @Path("/{id}/")
        public String get() {
            return "ok";
        }
    }

    @Path("/invalid")
    private static final class InvalidController {
        @GET
        public String get(String value) {
            return value;
        }
    }
}
