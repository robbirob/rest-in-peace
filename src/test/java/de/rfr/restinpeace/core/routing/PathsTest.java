package de.rfr.restinpeace.core.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathsTest {
    @Test
    void normalizeCollapsesExtraSlashesAndAddsLeadingSlash() {
        assertEquals("/users/123", Paths.normalize("users//123/"));
    }

    @Test
    void normalizeReturnsRootForBlankPath() {
        assertEquals("/", Paths.normalize(""));
    }
}
