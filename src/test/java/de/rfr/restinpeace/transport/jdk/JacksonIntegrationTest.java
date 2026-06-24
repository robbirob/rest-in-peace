package de.rfr.restinpeace.transport.jdk;

import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.api.annotations.Body;
import de.rfr.restinpeace.api.annotations.POST;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.json.jackson.JacksonBodyCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonIntegrationTest {
    private RestApp app;

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void postJsonBodyBindsToPojo() throws Exception {
        app = RestApp.builder()
            .register(new JsonController())
            .codec(new JacksonBodyCodec())
            .build();
        app.listen("127.0.0.1", 0);

        HttpResponse<String> response = request("POST", "/json", "{\"name\":\"alice\"}");

        assertEquals(200, response.statusCode());
        assertEquals("hello alice", response.body());
    }

    @Test
    void jacksonCodecRecognizesJsonMediaTypesPrecisely() {
        JacksonBodyCodec codec = new JacksonBodyCodec();

        assertTrue(codec.canRead(CreateRequest.class, "application/json; charset=utf-8"));
        assertTrue(codec.canRead(CreateRequest.class, "application/vnd.example+json; charset=utf-8"));
        assertFalse(codec.canRead(CreateRequest.class, "text/plain; note=application/json"));
    }

    private HttpResponse<String> request(String method, String path, String body) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + app.port() + path))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Path("/json")
    public static final class JsonController {
        @POST
        public String create(@Body CreateRequest request) {
            return "hello " + request.name();
        }
    }

    public record CreateRequest(String name) {
    }
}
