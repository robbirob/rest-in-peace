package de.rfr.restinpeace.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a URI path segment on a controller type or handler method.
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Path {
    /**
     * Returns the configured URI path segment.
     *
     * @return path segment for class-level or method-level mapping
     */
    String value();
}
