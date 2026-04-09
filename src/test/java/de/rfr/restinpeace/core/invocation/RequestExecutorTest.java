package de.rfr.restinpeace.core.invocation;

import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.api.annotations.Body;
import de.rfr.restinpeace.api.annotations.Consumes;
import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.api.annotations.POST;
import de.rfr.restinpeace.api.annotations.Produces;
import de.rfr.restinpeace.api.annotations.QueryParam;
import de.rfr.restinpeace.api.response.HttpResponse;
import de.rfr.restinpeace.core.http.FrameworkRequest;
import de.rfr.restinpeace.core.http.FrameworkResponse;
import de.rfr.restinpeace.core.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestExecutorTest {
    @Test
    void returns404WhenNoRouteMatches() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/unknown"));

        assertEquals(404, response.status());
        assertEquals("application/json", response.headers().get("Content-Type"));
        assertEquals("{\"code\":\"not_found\",\"message\":\"no route matched\"}", response.body());
    }

    @Test
    void returns405WhenPathMatchesButMethodDoesNot() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.POST, "/test/ping"));

        assertEquals(405, response.status());
        assertEquals("application/json", response.headers().get("Content-Type"));
        assertEquals("{\"code\":\"method_not_allowed\",\"message\":\"method not allowed\"}", response.body());
    }

    @Test
    void returns400OnInvalidQueryConversion() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(
            HttpMethod.GET,
            "/test/number",
            Map.of("value", List.of("not-a-number")),
            Map.of()
        ));

        assertEquals(400, response.status());
        assertEquals("application/json", response.headers().get("Content-Type"));
    }

    @Test
    void resolvesEnumAndOptionalParameters() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(
            HttpMethod.GET,
            "/test/mode",
            Map.of("mode", List.of("FAST"), "enabled", List.of("true")),
            Map.of()
        ));

        assertEquals(200, response.status());
        assertEquals("FAST:true", response.body());
    }

    @Test
    void mapsVoidReturnTo204() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/void"));

        assertEquals(204, response.status());
    }

    @Test
    void appliesCustomExceptionMapper() {
        RestApp app = RestApp.builder()
            .register(new TestController())
            .exception(IllegalStateException.class, ex -> HttpResponse.status(409, ex.getMessage()))
            .build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/error"));

        assertEquals(409, response.status());
        assertEquals("conflict", response.body());
    }

    @Test
    void keepsExplicitContentTypeWhenProducesIsSet() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/produce-explicit"));

        assertEquals(200, response.status());
        assertEquals("text/plain", response.headers().get("Content-Type"));
    }

    @Test
    void ignoresConsumesWithoutBodyParameter() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/consumes-no-body"));

        assertEquals(200, response.status());
        assertEquals("ok", response.body());
    }

    @Test
    void mapsUnhandledExceptionsTo500() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/unhandled"));

        assertEquals(500, response.status());
        assertEquals("application/json", response.headers().get("Content-Type"));
        assertEquals("{\"code\":\"internal_server_error\",\"message\":\"unhandled exception\"}", response.body());
    }

    @Test
    void enforcesConsumesForBodyHandlers() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(
            HttpMethod.POST,
            "/test/consume",
            Map.of(),
            Map.of(),
            "hello",
            "text/plain"
        ));

        assertEquals(415, response.status());
    }

    @Test
    void appliesProducesHeaderWhenNotExplicitlySet() {
        RestApp app = RestApp.builder().register(new TestController()).build();

        FrameworkResponse response = app.config().requestExecutor().execute(request(HttpMethod.GET, "/test/produce"));

        assertEquals(200, response.status());
        assertEquals("application/json", response.headers().get("Content-Type"));
    }

    private static FrameworkRequest request(HttpMethod method, String path) {
        return request(method, path, Map.of(), Map.of());
    }

    private static FrameworkRequest request(
        HttpMethod method,
        String path,
        Map<String, List<String>> query,
        Map<String, List<String>> headers
    ) {
        return new TestRequest(method, path, query, headers, new byte[0], firstHeader(headers, "Content-Type"));
    }

    private static FrameworkRequest request(
        HttpMethod method,
        String path,
        Map<String, List<String>> query,
        Map<String, List<String>> headers,
        String body,
        String contentType
    ) {
        return new TestRequest(
            method,
            path,
            query,
            headers,
            body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            contentType
        );
    }

    @Path("/test")
    private static final class TestController {
        @GET
        @Path("/ping")
        public String ping() {
            return "pong";
        }

        @GET
        @Path("/number")
        public String number(@QueryParam("value") int value) {
            return Integer.toString(value);
        }

        @GET
        @Path("/mode")
        public String mode(@QueryParam("mode") Mode mode, @QueryParam("enabled") Optional<Boolean> enabled) {
            return mode + ":" + enabled.orElse(false);
        }

        @GET
        @Path("/void")
        public void voidResponse() {
        }

        @GET
        @Path("/error")
        public String error() {
            throw new IllegalStateException("conflict");
        }

        @POST
        @Path("/consume")
        @Consumes("application/json")
        public String consume(@Body String body) {
            return body;
        }

        @GET
        @Path("/produce")
        @Produces("application/json")
        public String produce() {
            return "{\"ok\":true}";
        }

        @GET
        @Path("/produce-explicit")
        @Produces("application/json")
        public HttpResponse<String> produceExplicit() {
            return HttpResponse.ok("ok").header("Content-Type", "text/plain");
        }

        @GET
        @Path("/consumes-no-body")
        @Consumes("application/json")
        public String consumesNoBody() {
            return "ok";
        }

        @GET
        @Path("/unhandled")
        public String unhandled() {
            throw new RuntimeException("boom");
        }
    }

    private enum Mode {
        FAST,
        SAFE
    }

    private record TestRequest(
        HttpMethod method,
        String path,
        Map<String, List<String>> queryParams,
        Map<String, List<String>> headers,
        byte[] rawBody,
        String contentType
    ) implements FrameworkRequest {
        @Override
        public InputStream body() {
            return new ByteArrayInputStream(rawBody);
        }
    }

    private static String firstHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return null;
    }
}
