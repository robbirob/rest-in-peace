package de.rfr.restinpeace.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory user store used by sample controllers.
 */
public final class UserService {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    /**
     * Creates the sample service with one predefined user.
     */
    public UserService() {
        users.put("123", "demo-user");
    }

    /**
     * Looks up a user name by id.
     *
     * @param id user id
     * @return user name or {@code null} when not found
     */
    public String findById(String id) {
        return users.get(id);
    }

    /**
     * Creates a user and returns the generated id.
     *
     * @param name user name
     * @return generated user id
     */
    public String create(String name) {
        String id = Integer.toString(users.size() + 1);
        users.put(id, name);
        return id;
    }
}
