package de.rfr.restinpeace.core.binding;

import de.rfr.restinpeace.api.annotations.Body;
import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.HeaderParam;
import de.rfr.restinpeace.api.annotations.POST;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.api.annotations.PathParam;
import de.rfr.restinpeace.api.annotations.QueryParam;
import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.core.controller.ControllerInspector;
import de.rfr.restinpeace.core.controller.HandlerMethod;
import de.rfr.restinpeace.core.conversion.ConverterRegistry;
import de.rfr.restinpeace.core.error.BadRequestException;
import de.rfr.restinpeace.core.error.UnsupportedMediaTypeException;
import de.rfr.restinpeace.core.http.FrameworkRequest;
import de.rfr.restinpeace.core.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterBinderTest {
    @Test
    void handlesOptionalAndNullableScalars() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());
        HandlerMethod optionalHandler = handler("queryOptional");
        HandlerMethod nullableHandler = handler("queryNullable");

        Object[] optionalArgs = binder.bind(optionalHandler, request(), Map.of());
        Object[] nullableArgs = binder.bind(nullableHandler, request(), Map.of());

        assertEquals(Optional.empty(), optionalArgs[0]);
        assertNull(nullableArgs[0]);
    }

    @Test
    void failsOnMissingRequiredPathParam() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());
        HandlerMethod pathHandler = handler("pathRequired");

        assertThrows(BadRequestException.class, () -> binder.bind(pathHandler, request(), Map.of()));
    }

    @Test
    void resolvesHeadersCaseInsensitively() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());

        Object[] args = binder.bind(handler("header"), requestWithHeader("X-Request-Id", "abc"), Map.of());

        assertEquals("abc", args[0]);
    }

    @Test
    void readsByteArrayAndStringBody() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());

        Object[] bytesArgs = binder.bind(handler("bodyBytes"), requestWithBody("abc", "text/plain"), Map.of());
        Object[] textArgs = binder.bind(handler("bodyString"), requestWithBody("hello", "text/plain"), Map.of());

        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), (byte[]) bytesArgs[0]);
        assertEquals("hello", textArgs[0]);
    }

    @Test
    void returnsNullForEmptyCustomBody() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of(new PayloadCodec(false)));

        Object[] args = binder.bind(handler("bodyPayload"), requestWithBodyBytes(new byte[0], "application/json"), Map.of());

        assertNull(args[0]);
    }

    @Test
    void failsWhenNoCodecCanReadCustomBody() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());

        assertThrows(UnsupportedMediaTypeException.class, () ->
            binder.bind(handler("bodyPayload"), requestWithBody("{}", "application/json"), Map.of())
        );
    }

    @Test
    void wrapsCodecDecodeFailureAsBadRequest() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of(new PayloadCodec(true)));

        assertThrows(BadRequestException.class, () ->
            binder.bind(handler("bodyPayload"), requestWithBody("{}", "application/json"), Map.of())
        );
    }

    @Test
    void failsForRawOptionalParameter() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());

        assertThrows(BadRequestException.class, () ->
            binder.bind(handler("rawOptional"), requestWithQuery("q", "1"), Map.of())
        );
    }

    @Test
    void wrapsBodyReadIoErrors() {
        ParameterBinder binder = new ParameterBinder(new ConverterRegistry(), List.of());

        assertThrows(BadRequestException.class, () ->
            binder.bind(handler("bodyString"), failingBodyRequest(), Map.of())
        );
    }

    private static HandlerMethod handler(String methodName) {
        return new ControllerInspector().inspect(List.of(new BindingController())).stream()
            .map(route -> route.handlerMethod())
            .filter(handler -> handler.method().getName().equals(methodName))
            .findFirst()
            .orElseThrow();
    }

    private static FrameworkRequest request() {
        return new TestRequest(HttpMethod.GET, "/bind", Map.of(), Map.of(), new byte[0], null, false);
    }

    private static FrameworkRequest requestWithBody(String body, String contentType) {
        return requestWithBodyBytes(body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static FrameworkRequest requestWithBodyBytes(byte[] body, String contentType) {
        return new TestRequest(HttpMethod.POST, "/bind", Map.of(), Map.of(), body, contentType, false);
    }

    private static FrameworkRequest requestWithQuery(String name, String value) {
        return new TestRequest(HttpMethod.GET, "/bind", Map.of(name, List.of(value)), Map.of(), new byte[0], null, false);
    }

    private static FrameworkRequest requestWithHeader(String name, String value) {
        return new TestRequest(HttpMethod.GET, "/bind", Map.of(), Map.of(name, List.of(value)), new byte[0], null, false);
    }

    private static FrameworkRequest failingBodyRequest() {
        return new TestRequest(HttpMethod.POST, "/bind", Map.of(), Map.of(), new byte[0], "text/plain", true);
    }

    @Path("/bind")
    private static final class BindingController {
        @GET
        @Path("/path")
        public String pathRequired(@PathParam("id") int id) {
            return Integer.toString(id);
        }

        @GET
        @Path("/query-opt")
        public String queryOptional(@QueryParam("q") Optional<Integer> q) {
            return q.map(Object::toString).orElse("none");
        }

        @GET
        @Path("/query-null")
        public String queryNullable(@QueryParam("q") Integer q) {
            return q == null ? "none" : q.toString();
        }

        @GET
        @Path("/header")
        public String header(@HeaderParam("x-request-id") String requestId) {
            return requestId;
        }

        @POST
        @Path("/bytes")
        public String bodyBytes(@Body byte[] body) {
            return Integer.toString(body.length);
        }

        @POST
        @Path("/string")
        public String bodyString(@Body String body) {
            return body;
        }

        @POST
        @Path("/payload")
        public String bodyPayload(@Body Payload payload) {
            return payload == null ? "null" : payload.value();
        }

        @GET
        @Path("/raw-optional")
        public String rawOptional(@QueryParam("q") Optional q) {
            return q.toString();
        }
    }

    private record Payload(String value) {
    }

    private record TestRequest(
        HttpMethod method,
        String path,
        Map<String, List<String>> queryParams,
        Map<String, List<String>> headers,
        byte[] bodyBytes,
        String contentType,
        boolean failOnBodyRead
    ) implements FrameworkRequest {
        @Override
        public InputStream body() {
            if (failOnBodyRead) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom");
                    }
                };
            }
            return new ByteArrayInputStream(bodyBytes);
        }
    }

    private static final class PayloadCodec implements BodyCodec {
        private final boolean failDecode;

        private PayloadCodec(boolean failDecode) {
            this.failDecode = failDecode;
        }

        @Override
        public boolean canRead(Class<?> type, String contentType) {
            return type == Payload.class && contentType != null && contentType.contains("application/json");
        }

        @Override
        public boolean canWrite(Class<?> type, String contentType) {
            return false;
        }

        @Override
        public <T> T read(InputStream input, Class<T> type) throws Exception {
            if (failDecode) {
                throw new IOException("decode failed");
            }
            return type.cast(new Payload("ok"));
        }

        @Override
        public void write(OutputStream output, Object value) {
        }
    }
}
