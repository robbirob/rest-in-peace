package de.rfr.restinpeace.json.jackson;

import tools.jackson.databind.ObjectMapper;
import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.core.http.MediaTypes;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Optional Jackson-based {@link BodyCodec} implementation for JSON payloads.
 *
 * @since 0.1.0
 */
public final class JacksonBodyCodec implements BodyCodec {
    private final ObjectMapper objectMapper;

    /**
     * Creates a codec with a default {@link ObjectMapper}.
     */
    public JacksonBodyCodec() {
        this(new ObjectMapper());
    }

    /**
     * Creates a codec with a custom object mapper.
     *
     * @param objectMapper mapper used for serialization and deserialization
     */
    public JacksonBodyCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRead(Class<?> type, String contentType) {
        return isJson(contentType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canWrite(Class<?> type, String contentType) {
        return contentType == null || isJson(contentType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T read(InputStream input, Class<T> type) throws Exception {
        return objectMapper.readValue(input, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(OutputStream output, Object value) throws Exception {
        objectMapper.writeValue(output, value);
    }

    private static boolean isJson(String contentType) {
        return MediaTypes.isJson(contentType);
    }
}
