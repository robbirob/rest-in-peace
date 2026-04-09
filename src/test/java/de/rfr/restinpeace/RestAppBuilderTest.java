package de.rfr.restinpeace;

import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.api.error.ExceptionMapper;
import de.rfr.restinpeace.api.response.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestAppBuilderTest {
    @Test
    void builderStoresRegisteredComponents() {
        Object controller = new Object();
        BodyCodec codec = new NoOpCodec();
        ExceptionMapper<IllegalArgumentException> mapper = ex -> HttpResponse.status(400, ex.getMessage());

        RestApp app = RestApp.builder()
            .register(controller)
            .codec(codec)
            .exception(IllegalArgumentException.class, mapper)
            .build();

        assertEquals(1, app.config().controllers().size());
        assertEquals(1, app.config().codecs().size());
        assertEquals(1, app.config().exceptionMappers().size());
    }

    private static final class NoOpCodec implements BodyCodec {
        @Override
        public boolean canRead(Class<?> type, String contentType) {
            return false;
        }

        @Override
        public boolean canWrite(Class<?> type, String contentType) {
            return false;
        }

        @Override
        public <T> T read(InputStream input, Class<T> type) {
            return null;
        }

        @Override
        public void write(OutputStream output, Object value) {
        }
    }
}
