package de.rfr.restinpeace.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the response content type produced by a handler method.
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Produces {
    /**
     * Returns the response content type value.
     *
     * @return produced response content type
     */
    String value();
}
