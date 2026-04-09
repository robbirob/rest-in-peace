package de.rfr.restinpeace.core.error;

import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.api.response.HttpResponse;

import java.util.List;
import java.util.Optional;

/**
 * Resolves user-registered exception mappers for thrown exceptions.
 */
public final class ExceptionMapperRegistry {
    private final List<RestApp.ExceptionMapperRegistration<?>> registrations;

    /**
     * Creates a mapper registry from configured mapper registrations.
     *
     * @param registrations mapper registrations from the application builder
     */
    public ExceptionMapperRegistry(List<RestApp.ExceptionMapperRegistration<?>> registrations) {
        this.registrations = List.copyOf(registrations);
    }

    /**
     * Attempts to map the given throwable with a registered mapper.
     *
     * @param throwable throwable to map
     * @return mapped response when a mapper matches, otherwise empty
     */
    public Optional<HttpResponse<?>> map(Throwable throwable) {
        for (RestApp.ExceptionMapperRegistration<?> registration : registrations) {
            if (registration.type().isAssignableFrom(throwable.getClass())) {
                return Optional.of(invokeMapper(registration, throwable));
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> HttpResponse<?> invokeMapper(
        RestApp.ExceptionMapperRegistration<?> registration,
        Throwable throwable
    ) {
        RestApp.ExceptionMapperRegistration<E> typed = (RestApp.ExceptionMapperRegistration<E>) registration;
        return typed.mapper().map((E) throwable);
    }
}
