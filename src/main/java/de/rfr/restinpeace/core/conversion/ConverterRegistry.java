package de.rfr.restinpeace.core.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of scalar converters used by request parameter binding.
 */
public final class ConverterRegistry {
    private final Map<Class<?>, Converter<?>> converters = new HashMap<>();

    /**
     * Creates a new registry preloaded with default scalar converters.
     */
    public ConverterRegistry() {
        registerDefaults();
    }

    /**
     * Registers a converter for a target Java type.
     *
     * @param type target type
     * @param converter converter implementation
     * @param <T> type parameter
     */
    public <T> void register(Class<T> type, Converter<T> converter) {
        converters.put(Objects.requireNonNull(type, "type must not be null"),
            Objects.requireNonNull(converter, "converter must not be null"));
    }

    /**
     * Converts a raw text value into a target type.
     *
     * @param value raw value
     * @param targetType target class
     * @return converted object
     * @throws ConversionException when no converter exists or conversion fails
     */
    public Object convert(String value, Class<?> targetType) {
        Class<?> effectiveType = wrapPrimitive(targetType);

        if (effectiveType == String.class) {
            return value;
        }

        if (effectiveType.isEnum()) {
            return convertEnum(value, effectiveType);
        }

        Converter<?> converter = converters.get(effectiveType);
        if (converter == null) {
            throw new ConversionException("no converter registered for type: " + effectiveType.getName());
        }

        try {
            return converter.convert(value);
        } catch (Exception ex) {
            throw new ConversionException("failed to convert value '" + value + "' to " + effectiveType.getSimpleName(), ex);
        }
    }

    private Object convertEnum(String value, Class<?> enumType) {
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object converted = Enum.valueOf((Class<? extends Enum>) enumType.asSubclass(Enum.class), value);
            return converted;
        } catch (IllegalArgumentException ex) {
            throw new ConversionException("failed to convert value '" + value + "' to " + enumType.getSimpleName(), ex);
        }
    }

    private void registerDefaults() {
        register(Boolean.class, Boolean::parseBoolean);
        register(Byte.class, Byte::parseByte);
        register(Short.class, Short::parseShort);
        register(Integer.class, Integer::parseInt);
        register(Long.class, Long::parseLong);
        register(Float.class, Float::parseFloat);
        register(Double.class, Double::parseDouble);
        register(Character.class, value -> {
            if (value.length() != 1) {
                throw new IllegalArgumentException("expected single character");
            }
            return value.charAt(0);
        });
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
