package de.rfr.restinpeace.core.conversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterRegistryTest {
    @Test
    void convertsPrimitiveTargets() {
        ConverterRegistry registry = new ConverterRegistry();

        assertEquals(true, registry.convert("true", boolean.class));
        assertEquals((byte) 7, registry.convert("7", byte.class));
        assertEquals((short) 8, registry.convert("8", short.class));
        assertEquals(9, registry.convert("9", int.class));
        assertEquals(10L, registry.convert("10", long.class));
        assertEquals(1.5f, registry.convert("1.5", float.class));
        assertEquals(2.5d, registry.convert("2.5", double.class));
        assertEquals('x', registry.convert("x", char.class));
    }

    @Test
    void convertsStringsAndEnums() {
        ConverterRegistry registry = new ConverterRegistry();

        assertEquals("raw", registry.convert("raw", String.class));
        assertEquals(Level.HIGH, registry.convert("HIGH", Level.class));
    }

    @Test
    void throwsOnInvalidEnumAndUnknownType() {
        ConverterRegistry registry = new ConverterRegistry();

        assertThrows(ConversionException.class, () -> registry.convert("MIDDLE", Level.class));
        assertThrows(ConversionException.class, () -> registry.convert("anything", Object.class));
    }

    @Test
    void throwsOnInvalidCharacterValue() {
        ConverterRegistry registry = new ConverterRegistry();

        assertThrows(ConversionException.class, () -> registry.convert("too-long", char.class));
    }

    @Test
    void supportsCustomRegistration() {
        ConverterRegistry registry = new ConverterRegistry();
        registry.register(Custom.class, value -> new Custom(value.toUpperCase()));

        Custom converted = (Custom) registry.convert("abc", Custom.class);
        assertEquals("ABC", converted.value());
    }

    private enum Level {
        LOW,
        HIGH
    }

    private record Custom(String value) {
    }
}
