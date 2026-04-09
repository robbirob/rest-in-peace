package de.rfr.restinpeace.example;

import de.rfr.restinpeace.api.annotations.Body;
import de.rfr.restinpeace.api.annotations.GET;
import de.rfr.restinpeace.api.annotations.POST;
import de.rfr.restinpeace.api.annotations.Path;
import de.rfr.restinpeace.api.annotations.PathParam;
import de.rfr.restinpeace.api.response.HttpResponse;

/**
 * Sample user controller used in quickstart and integration tests.
 */
@Path("/users")
public final class UserController {
    private final UserService userService;

    /**
     * Creates a controller backed by the given service.
     *
     * @param userService user service dependency
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Fetches a user by id.
     *
     * @param id user id path value
     * @return response containing found user or a 404 marker string
     */
    @GET
    @Path("/{id}")
    public HttpResponse<String> get(@PathParam("id") String id) {
        String user = userService.findById(id);
        if (user == null) {
            return HttpResponse.status(404, "user_not_found");
        }
        return HttpResponse.ok(id + ":" + user);
    }

    /**
     * Creates a user from plain text body content.
     *
     * @param body request body containing user name
     * @return created response with generated identifier
     */
    @POST
    public HttpResponse<String> create(@Body String body) {
        String name = body == null ? "" : body.trim();
        if (name.isEmpty()) {
            return HttpResponse.status(400, "name_required");
        }
        String id = userService.create(name);
        return HttpResponse.created(id + ":" + name)
            .header("Location", "/users/" + id);
    }
}
