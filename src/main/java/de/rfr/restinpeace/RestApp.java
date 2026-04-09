package de.rfr.restinpeace;

import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.api.error.ExceptionMapper;
import de.rfr.restinpeace.core.AppConfig;
import de.rfr.restinpeace.core.binding.ParameterBinder;
import de.rfr.restinpeace.core.controller.ControllerInspector;
import de.rfr.restinpeace.core.conversion.ConverterRegistry;
import de.rfr.restinpeace.core.error.ExceptionMapperRegistry;
import de.rfr.restinpeace.core.invocation.RequestExecutor;
import de.rfr.restinpeace.core.routing.RouteDefinition;
import de.rfr.restinpeace.core.routing.RouteRegistry;
import de.rfr.restinpeace.transport.jdk.JdkHttpServerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Main framework entry point used to configure controllers, codecs, and server lifecycle.
 *
 * @since 0.1.0
 */
public final class RestApp {
    private final AppConfig config;
    private final JdkHttpServerAdapter transport = new JdkHttpServerAdapter();
    private volatile JdkHttpServerAdapter.RunningServer runningServer;

    private RestApp(AppConfig config) {
        this.config = config;
    }

    /**
     * Creates a new fluent builder for constructing a {@link RestApp} instance.
     *
     * @return new application builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the internal immutable application configuration.
     *
     * @return app configuration object
     */
    public AppConfig config() {
        return config;
    }

    /**
     * Starts the HTTP server on all interfaces.
     *
     * @param port TCP port to bind
     */
    public synchronized void listen(int port) {
        listen("0.0.0.0", port);
    }

    /**
     * Starts the HTTP server for this application.
     *
     * @param host host or interface to bind
     * @param port TCP port to bind
     * @throws IllegalArgumentException when port is outside {@code 0..65535}
     * @throws IllegalStateException when server is already running
     */
    public synchronized void listen(String host, int port) {
        Objects.requireNonNull(host, "host must not be null");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (runningServer != null) {
            throw new IllegalStateException("server is already running");
        }
        runningServer = transport.start(config, host, port);
    }

    /**
     * Stops the server when it is running.
     */
    public synchronized void stop() {
        if (runningServer == null) {
            return;
        }
        transport.stop(runningServer);
        runningServer = null;
    }

    /**
     * Returns the currently bound port.
     *
     * @return bound port or {@code -1} when server is not running
     */
    public int port() {
        JdkHttpServerAdapter.RunningServer current = runningServer;
        return current == null ? -1 : current.port();
    }

    /**
     * Fluent builder for creating a {@link RestApp}.
     *
     * @since 0.1.0
     */
    public static final class Builder {
        private final List<Object> controllers = new ArrayList<>();
        private final List<BodyCodec> codecs = new ArrayList<>();
        private final List<ExceptionMapperRegistration<?>> exceptionMappers = new ArrayList<>();
        private Executor executor;

        /**
         * Creates a new application builder.
         */
        public Builder() {
        }

        /**
         * Registers a controller instance.
         *
         * @param controller controller object containing annotated handler methods
         * @return this builder instance
         */
        public Builder register(Object controller) {
            Objects.requireNonNull(controller, "controller must not be null");
            controllers.add(controller);
            return this;
        }

        /**
         * Registers a body codec used for request/response serialization.
         *
         * @param codec codec implementation
         * @return this builder instance
         */
        public Builder codec(BodyCodec codec) {
            Objects.requireNonNull(codec, "codec must not be null");
            codecs.add(codec);
            return this;
        }

        /**
         * Registers a custom exception mapper.
         *
         * @param type mapped exception type
         * @param mapper mapper implementation
         * @param <E> exception type
         * @return this builder instance
         */
        public <E extends Throwable> Builder exception(Class<E> type, ExceptionMapper<E> mapper) {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(mapper, "mapper must not be null");
            exceptionMappers.add(new ExceptionMapperRegistration<>(type, mapper));
            return this;
        }

        /**
         * Sets the executor used by the JDK HTTP transport.
         *
         * @param executor request executor
         * @return this builder instance
         */
        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor must not be null");
            return this;
        }

        /**
         * Builds and validates a configured {@link RestApp} instance.
         *
         * @return initialized application
         * @throws IllegalArgumentException when controller metadata is invalid
         */
        public RestApp build() {
            List<Object> registeredControllers = List.copyOf(controllers);
            List<BodyCodec> configuredCodecs = List.copyOf(codecs);
            List<ExceptionMapperRegistration<?>> configuredExceptionMappers = List.copyOf(exceptionMappers);

            ControllerInspector inspector = new ControllerInspector();
            List<RouteDefinition> routes = inspector.inspect(registeredControllers);
            RouteRegistry routeRegistry = new RouteRegistry(routes);
            ConverterRegistry converterRegistry = new ConverterRegistry();
            ParameterBinder parameterBinder = new ParameterBinder(converterRegistry, configuredCodecs);
            ExceptionMapperRegistry mapperRegistry = new ExceptionMapperRegistry(configuredExceptionMappers);
            RequestExecutor requestExecutor = new RequestExecutor(routeRegistry, parameterBinder, mapperRegistry);

            return new RestApp(new AppConfig(
                registeredControllers,
                configuredCodecs,
                configuredExceptionMappers,
                executor,
                routeRegistry,
                requestExecutor
            ));
        }
    }

    /**
     * Typed registration entry for a custom exception mapper.
     *
     * @param type mapped exception class
     * @param mapper mapper callback
     * @param <E> exception type
     * @since 0.1.0
     */
    public record ExceptionMapperRegistration<E extends Throwable>(
        Class<E> type,
        ExceptionMapper<E> mapper
    ) {
    }
}
