package de.rfr.restinpeace.core.binding;

import de.rfr.restinpeace.api.codec.BodyCodec;
import de.rfr.restinpeace.core.controller.HandlerMethod;
import de.rfr.restinpeace.core.controller.ParameterDescriptor;
import de.rfr.restinpeace.core.controller.ParameterKind;
import de.rfr.restinpeace.core.conversion.ConversionException;
import de.rfr.restinpeace.core.conversion.ConverterRegistry;
import de.rfr.restinpeace.core.error.BadRequestException;
import de.rfr.restinpeace.core.error.UnsupportedMediaTypeException;
import de.rfr.restinpeace.core.http.FrameworkRequest;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves invocation arguments from path/query/header/body request data.
 */
public final class ParameterBinder {
    private final ConverterRegistry converterRegistry;
    private final List<BodyCodec> codecs;

    /**
     * Creates a new parameter binder.
     *
     * @param converterRegistry scalar converter registry
     * @param codecs available body codecs
     */
    public ParameterBinder(ConverterRegistry converterRegistry, List<BodyCodec> codecs) {
        this.converterRegistry = Objects.requireNonNull(converterRegistry, "converterRegistry must not be null");
        this.codecs = List.copyOf(codecs);
    }

    /**
     * Binds all handler parameters for a route match.
     *
     * @param handler handler metadata
     * @param request request abstraction
     * @param pathParams extracted path variables
     * @return invocation argument array in declaration order
     */
    public Object[] bind(HandlerMethod handler, FrameworkRequest request, Map<String, String> pathParams) {
        Object[] args = new Object[handler.parameters().size()];

        for (ParameterDescriptor parameter : handler.parameters()) {
            args[parameter.index()] = resolveParameter(parameter, handler, request, pathParams);
        }

        return args;
    }

    private Object resolveParameter(
        ParameterDescriptor parameter,
        HandlerMethod handler,
        FrameworkRequest request,
        Map<String, String> pathParams
    ) {
        return switch (parameter.kind()) {
            case PATH -> resolveScalar(pathParams.get(parameter.name()), parameter, true);
            case QUERY -> resolveScalar(first(request.queryParams().get(parameter.name())), parameter, false);
            case HEADER -> resolveScalar(first(request.headers().get(parameter.name())), parameter, false);
            case BODY -> resolveBody(request, parameter.type());
            case UNANNOTATED -> throw new BadRequestException("unannotated parameter is not supported: " + parameter.parameter());
        };
    }

    private Object resolveScalar(String rawValue, ParameterDescriptor parameter, boolean requiredAlways) {
        boolean optional = parameter.type() == Optional.class;
        Class<?> targetType = optional ? optionalType(parameter) : parameter.type();
        boolean required = requiredAlways || parameter.type().isPrimitive();

        if (rawValue == null) {
            if (optional) {
                return Optional.empty();
            }
            if (required) {
                throw new BadRequestException("missing required parameter: " + parameter.name());
            }
            return null;
        }

        try {
            Object converted = converterRegistry.convert(rawValue, targetType);
            return optional ? Optional.of(converted) : converted;
        } catch (ConversionException ex) {
            throw new BadRequestException("invalid value for parameter: " + parameter.name(), ex);
        }
    }

    private Object resolveBody(FrameworkRequest request, Class<?> bodyType) {
        try {
            byte[] bytes = request.body().readAllBytes();
            if (bodyType == byte[].class) {
                return bytes;
            }
            if (bodyType == String.class) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            if (bytes.length == 0) {
                return null;
            }

            String contentType = request.contentType();
            for (BodyCodec codec : codecs) {
                if (codec.canRead(bodyType, contentType)) {
                    return codec.read(new java.io.ByteArrayInputStream(bytes), bodyType);
                }
            }

            throw new UnsupportedMediaTypeException("no body codec available for type "
                + bodyType.getName() + " and content type " + contentType);
        } catch (IOException ex) {
            throw new BadRequestException("failed to read request body", ex);
        } catch (UnsupportedMediaTypeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("failed to decode request body", ex);
        }
    }

    private static String first(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    private static Class<?> optionalType(ParameterDescriptor parameter) {
        Type type = parameter.parameter().getParameterizedType();
        if (!(type instanceof ParameterizedType parameterizedType)) {
            throw new BadRequestException("Optional parameter must declare a type argument: " + parameter.parameter());
        }
        Type argument = parameterizedType.getActualTypeArguments()[0];
        if (argument instanceof Class<?> argumentClass) {
            return argumentClass;
        }
        throw new BadRequestException("unsupported Optional type argument: " + argument);
    }
}
