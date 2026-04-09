package de.rfr.restinpeace.api.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpResponseTest {
    @Test
    void noContentDefaultsTo204AndNullBody() {
        HttpResponse<Void> response = HttpResponse.noContent();

        assertEquals(204, response.status());
        assertNull(response.body());
    }

    @Test
    void headerReturnsNewInstanceWithAddedHeader() {
        HttpResponse<String> response = HttpResponse.ok("ok").header("X-Test", "true");

        assertEquals(200, response.status());
        assertEquals("true", response.headers().get("X-Test"));
    }
}
