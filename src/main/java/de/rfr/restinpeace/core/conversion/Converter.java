package de.rfr.restinpeace.core.conversion;

/**
 * Converts a string value to a specific Java type.
 *
 * @param <T> target type
 */
@FunctionalInterface
public interface Converter<T> {
    /**
     * Converts a raw text value.
     *
     * @param input raw input string
     * @return converted value
     * @throws Exception when conversion fails
     */
    T convert(String input) throws Exception;
}
