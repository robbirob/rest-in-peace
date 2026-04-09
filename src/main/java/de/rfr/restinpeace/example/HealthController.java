package de.rfr.restinpeace.example;

import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.Path;

/**
 * Sample controller exposing a health endpoint.
 */
@Path("/health")
public final class HealthController {
    /**
     * Creates the health controller.
     */
    public HealthController() {
    }

    /**
     * Returns a basic health probe response.
     *
     * @return static health status value
     */
    @GET
    public String health() {
        return "ok";
    }
}
