package de.rfr.restinpeace.example;

import de.rfr.restinpeace.RestApp;

/**
 * Runnable sample bootstrap for local manual testing.
 */
public final class Main {
    private Main() {
    }

    /**
     * Starts the sample application on port 8080.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        UserService userService = new UserService();

        RestApp app = RestApp.builder()
            .register(new HealthController())
            .register(new UserController(userService))
            .build();

        app.listen("0.0.0.0", 8080);
        System.out.println("rest-in-peace listening on port 8080");
    }
}
