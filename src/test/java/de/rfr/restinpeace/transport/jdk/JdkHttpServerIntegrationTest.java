package de.rfr.restinpeace.transport.jdk;

import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.example.HealthController;
import de.rfr.restinpeace.example.UserController;
import de.rfr.restinpeace.example.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdkHttpServerIntegrationTest {
    private RestApp app;

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void healthEndpointReturns200() throws Exception {
        startApp();

        HttpResponse<String> response = request("GET", "/health", null);

        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        assertEquals("text/plain; charset=utf-8", response.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    void usersPathParamEndpointReturns200() throws Exception {
        startApp();

        HttpResponse<String> response = request("GET", "/users/123", null);

        assertEquals(200, response.statusCode());
        assertEquals("123:demo-user", response.body());
    }

    @Test
    void usersCreateEndpointReadsBody() throws Exception {
        startApp();

        HttpResponse<String> response = request("POST", "/users", "alice");

        assertEquals(201, response.statusCode());
        assertEquals("text/plain; charset=utf-8", response.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    void unknownPathReturns404() throws Exception {
        startApp();

        HttpResponse<String> response = request("GET", "/unknown", null);

        assertEquals(404, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals("{\"code\":\"not_found\",\"message\":\"no route matched\"}", response.body());
    }

    @Test
    void wrongMethodReturns405() throws Exception {
        startApp();

        HttpResponse<String> response = request("PUT", "/health", null);

        assertEquals(405, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals("{\"code\":\"method_not_allowed\",\"message\":\"method not allowed\"}", response.body());
    }

    @Test
    void unsupportedHttpVerbReturns405() throws Exception {
        startApp();

        HttpResponse<String> response = request("PATCH", "/health", null);

        assertEquals(405, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    void byteArrayResponsesUseOctetStreamContentType() throws Exception {
        startApp();

        HttpResponse<String> response = request("GET", "/types/bytes", null);

        assertEquals(200, response.statusCode());
        assertEquals("application/octet-stream", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals("abc", response.body());
    }

    @Test
    void objectResponsesDefaultToJsonContentType() throws Exception {
        startApp();

        HttpResponse<String> response = request("GET", "/types/object", null);

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals("Value[value=test]", response.body());
    }

    private void startApp() {
        app = RestApp.builder()
            .register(new HealthController())
            .register(new UserController(new UserService()))
            .register(new TypeController())
            .build();
        app.listen("127.0.0.1", 0);
    }

    private HttpResponse<String> request(String method, String path, String body) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + app.port() + path));

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "text/plain");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Path("/types")
    public static final class TypeController {
        @GET
        @Path("/bytes")
        public byte[] bytes() {
            return "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @GET
        @Path("/object")
        public de.rfr.restinpeace.api.response.HttpResponse<Value> object() {
            return de.rfr.restinpeace.api.response.HttpResponse.ok(new Value("test"));
        }
    }

    public record Value(String value) {
    }
}
