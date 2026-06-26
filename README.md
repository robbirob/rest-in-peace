# rest-in-peace

`rest-in-peace` is a small annotation-based REST framework for Java with a strict focus on simplicity and minimal external dependencies.


## Requirements

- Java 21+
- Maven 3.9+

## Design goals

- Keep the core runtime JDK-only
- Keep the public API compact and explicit
- Prefer startup-time validation over runtime surprises
- Avoid framework magic and heavy abstractions

## Current state

- Maven project initialized
- Public API scaffolding added:
  - annotations (`@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PathParam`, `@QueryParam`, `@HeaderParam`, `@Body`, `@Produces`, `@Consumes`)
  - `HttpResponse<T>`
  - `BodyCodec`
  - `ExceptionMapper`
  - `RestApp` builder surface
- Startup route discovery and validation added:
  - class + method `@Path` composition and normalization
  - route template parsing (for example, `/users/{id}`)
  - route precedence (`/users/me` wins over `/users/{id}`)
  - ambiguous route detection at startup
  - handler validation (public methods, single HTTP method annotation, max one `@Body`)
- Unit tests now cover routing and controller inspection in addition to response/builder basics

## Quickstart

Canonical flow: build -> run -> curl.

### 1) Build and test

```bash
mvn clean test
```

### Verify locally

```bash
mvn test
```

### 2) Run the sample app

```bash
mvn -q compile
java -cp target/classes de.rfr.restinpeace.example.Main
```

### 3) Verify with curl (plain text sample)

```bash
curl http://localhost:8080/health
curl http://localhost:8080/users/123
curl -X POST http://localhost:8080/users -H "Content-Type: text/plain" --data "alice"
```

## Usage

The framework includes a working JDK `HttpServer` transport.

```java
import de.rfr.restinpeace.RestApp;
import de.rfr.restinpeace.example.HealthController;
import de.rfr.restinpeace.example.UserController;
import de.rfr.restinpeace.example.UserService;

public final class Main {
    public static void main(String[] args) {
        UserService userService = new UserService();

        RestApp app = RestApp.builder()
            .register(new HealthController())
            .register(new UserController(userService))
            .build();

        app.listen(8080);
    }
}
```

### What you can use now

- Annotation package for controller method metadata
- `HttpResponse<T>` helpers: `ok`, `created`, `noContent`, `status`
- `BodyCodec` SPI for pluggable body serialization
- `ExceptionMapper` registration in `RestApp.builder()`
- Startup-time route registry build and validation during `RestApp.builder().build()`
- JDK HTTP transport via `app.listen(host, port)` / `app.listen(port)` and `app.stop()`

### JSON example with optional Jackson codec

Add codec registration in your bootstrap:

```java
import de.rfr.restinpeace.json.jackson.JacksonBodyCodec;

RestApp app = RestApp.builder()
    .register(new JsonController())
    .codec(new JacksonBodyCodec())
    .build();
```

Then call an endpoint with JSON body:

```bash
curl -X POST http://localhost:8080/json \
  -H "Content-Type: application/json" \
  --data "{\"name\":\"alice\"}"
```

## Current limitations

- No package scanning for controllers (explicit `.register(...)` only)
- No DI container integration
- Basic content negotiation only (`@Consumes`/`@Produces` are intentionally minimal)

## License

MIT. See `LICENSE`.

## API Documentation

Generate Javadocs locally:

```bash
mvn javadoc:javadoc
```

Open: `target/site/apidocs/index.html`

## Status

- The `v0.1.x` line is available with JDK transport, startup validation, binding/conversion, and optional Jackson integration.
