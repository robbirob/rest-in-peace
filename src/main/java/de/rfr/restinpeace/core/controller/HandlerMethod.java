package de.rfr.restinpeace.core.controller;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Compiled handler metadata used during invocation.
 *
 * @param controller controller instance
 * @param method reflected handler method
 * @param parameters resolved parameter descriptors
 * @param consumes optional consumed content type
 * @param produces optional produced content type
 */
public record HandlerMethod(
    Object controller,
    Method method,
    List<ParameterDescriptor> parameters,
    String consumes,
    String produces
) {
}
