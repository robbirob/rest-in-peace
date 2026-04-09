package de.rfr.restinpeace.core.controller;

import java.lang.reflect.Parameter;

/**
 * Immutable parameter metadata for a compiled handler method.
 *
 * @param index parameter index in method signature
 * @param kind binding kind
 * @param name binding source name, when applicable
 * @param type runtime parameter type
 * @param parameter reflection parameter
 */
public record ParameterDescriptor(
    int index,
    ParameterKind kind,
    String name,
    Class<?> type,
    Parameter parameter
) {
}
