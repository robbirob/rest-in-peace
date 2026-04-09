package de.rfr.restinpeace.transport.jdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.core.AppConfig;
import de.rfr.restinpeace.core.http.FrameworkError;
import de.rfr.restinpeace.core.http.FrameworkRequest;
import de.rfr.restinpeace.core.http.FrameworkResponse;
import de.rfr.restinpeace.core.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thin transport adapter that bridges JDK {@link HttpServer} with framework request execution.
 */
public final class JdkHttpServerAdapter {
    /**
     * Creates a new JDK transport adapter.
     */
    public JdkHttpServerAdapter() {
    }

    /**
     * Starts an HTTP server bound to the given host and port.
     *
     * @param config application runtime configuration
     * @param host host/interface to bind
     * @param port TCP port to bind
     * @return running server handle
     */
    public RunningServer start(AppConfig config, String host, int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
            if (config.executor() != null) {
                server.setExecutor(config.executor());
            }

            server.createContext("/", exchange -> handleExchange(config, exchange));
            server.start();
            return new RunningServer(server, server.getAddress().getPort());
        } catch (IOException ex) {
            throw new RuntimeException("failed to start HTTP server", ex);
        }
    }

    /**
     * Stops the running server immediately.
     *
     * @param runningServer running server handle
     */
    public void stop(RunningServer runningServer) {
        runningServer.server().stop(0);
    }

    private void handleExchange(AppConfig config, HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        try {
            FrameworkRequest request = toFrameworkRequest(exchange, requestBody);
            FrameworkResponse response = config.requestExecutor().execute(request);
            writeResponse(exchange, response, config.codecs());
        } catch (IllegalArgumentException ex) {
            FrameworkResponse response = new FrameworkResponse(
                405,
                Map.of("Content-Type", "application/json"),
                new FrameworkError("method_not_allowed", "method not allowed")
            );
            writeResponse(exchange, response, config.codecs());
        }
    }

    private static FrameworkRequest toFrameworkRequest(HttpExchange exchange, byte[] requestBody) {
        HttpMethod method = parseMethod(exchange.getRequestMethod());
        URI uri = exchange.getRequestURI();

        Map<String, List<String>> queryParams = parseQuery(uri.getRawQuery());
        Map<String, List<String>> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, List.copyOf(values)));

        return new ExchangeRequest(
            method,
            uri.getPath(),
            queryParams,
            headers,
            requestBody,
            firstHeader(exchange.getRequestHeaders(), "Content-Type")
        );
    }

    private static HttpMethod parseMethod(String method) {
        return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
    }

    private static void writeResponse(HttpExchange exchange, FrameworkResponse response, List<BodyCodec> codecs) throws IOException {
        Map<String, String> responseHeaders = withDefaultContentType(response.headers(), response.body());
        byte[] bodyBytes = serializeBody(response.body(), responseHeaders, codecs);
        responseHeaders.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        exchange.sendResponseHeaders(response.status(), bodyBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bodyBytes);
        }
    }

    private static Map<String, String> withDefaultContentType(Map<String, String> headers, Object body) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(headers);
        if (hasHeader(result, "Content-Type") || body == null) {
            return result;
        }

        if (body instanceof String) {
            result.put("Content-Type", "text/plain; charset=utf-8");
            return result;
        }

        if (body instanceof byte[]) {
            result.put("Content-Type", "application/octet-stream");
            return result;
        }

        result.put("Content-Type", "application/json");
        return result;
    }

    private static byte[] serializeBody(Object body, Map<String, String> headers, List<BodyCodec> codecs) {
        if (body == null) {
            return new byte[0];
        }

        if (body instanceof byte[] bytes) {
            return bytes;
        }

        if (body instanceof String text) {
            return text.getBytes(StandardCharsets.UTF_8);
        }

        if (body instanceof FrameworkError error) {
            String json = "{\"code\":\"" + escapeJson(error.code()) + "\",\"message\":\"" + escapeJson(error.message()) + "\"}";
            return json.getBytes(StandardCharsets.UTF_8);
        }

        String contentType = firstHeaderValue(headers, "Content-Type");
        for (BodyCodec codec : codecs) {
            if (codec.canWrite(body.getClass(), contentType)) {
                try {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    codec.write(output, body);
                    return output.toByteArray();
                } catch (Exception ex) {
                    throw new RuntimeException("failed to encode response body", ex);
                }
            }
        }

        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') {
                builder.append('\\');
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static String firstHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                if (!entry.getValue().isEmpty()) {
                    return entry.getValue().getFirst();
                }
                return null;
            }
        }
        return null;
    }

    private static String firstHeaderValue(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean hasHeader(Map<String, String> headers, String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<String>> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";

            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8);

            result.computeIfAbsent(decodedKey, ignored -> new ArrayList<>()).add(decodedValue);
        }

        LinkedHashMap<String, List<String>> immutable = new LinkedHashMap<>();
        result.forEach((key, values) -> immutable.put(key, List.copyOf(values)));
        return Map.copyOf(immutable);
    }

    /**
     * Running JDK server handle with resolved bind port.
     *
     * @param server underlying server instance
     * @param port resolved bound port
     */
    public record RunningServer(HttpServer server, int port) {
    }

    private record ExchangeRequest(
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
}
