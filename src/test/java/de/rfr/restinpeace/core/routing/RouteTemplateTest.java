package de.rfr.restinpeace.core.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTemplateTest {
    @Test
    void parsesLiteralAndVariableSegments() {
        RouteTemplate template = RouteTemplate.parse("/users/{id}");

        assertEquals("/users/{id}", template.path());
        assertEquals(2, template.segmentCount());
        assertFalse(template.isVariableSegment(0));
        assertTrue(template.isVariableSegment(1));
        assertEquals("id", template.variableName(1));
        assertEquals("/users/{}", template.canonicalPattern());
    }
}
