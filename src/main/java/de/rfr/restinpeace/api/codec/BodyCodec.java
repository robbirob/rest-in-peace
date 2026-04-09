package de.rfr.restinpeace.api.codec;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Strategy interface for decoding request bodies and encoding response bodies.
 *
 * @since 0.1.0
 */
public interface BodyCodec {
    /**
     * Returns whether this codec can read the given target type for the provided content type.
     *
     * @param type target Java type
     * @param contentType request content type header value
     * @return {@code true} when this codec can decode the body
     */
    boolean canRead(Class<?> type, String contentType);

    /**
     * Returns whether this codec can write the given source type for the provided content type.
     *
     * @param type source Java type
     * @param contentType response content type header value
     * @return {@code true} when this codec can encode the body
     */
    boolean canWrite(Class<?> type, String contentType);

    /**
     * Decodes bytes from the input stream into the requested Java type.
     *
     * @param input request body stream
     * @param type target Java type
     * @param <T> result type
     * @return decoded value
     * @throws Exception when decoding fails
     */
    <T> T read(InputStream input, Class<T> type) throws Exception;

    /**
     * Encodes the given Java value to the output stream.
     *
     * @param output response output stream
     * @param value value to encode
     * @throws Exception when encoding fails
     */
    void write(OutputStream output, Object value) throws Exception;
}
