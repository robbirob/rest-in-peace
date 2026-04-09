# Changelog

## Unreleased

- Add GitHub Actions CI workflow for pushes and pull requests to `development`.
- Add tag-driven GitHub release workflow that uploads built JAR artifacts.

## v0.1.1 - 2026-04-09

### Build and distribution

- Configure Maven to attach `-sources.jar` and `-javadoc.jar` during `mvn install`.
- Improve IDE consumption from local Maven cache by making sources and javadocs consistently available.

## v0.1.0 - 2026-04-09

### Highlights

- Added `RestApp` bootstrap API with explicit controller registration and startup route validation.
- Implemented core routing, parameter binding, scalar conversion, invocation pipeline, and default error mapping.
- Added JDK `HttpServer` transport with sample endpoints (`/health`, `/users/{id}`, `POST /users`).
- Added optional Jackson integration through `JacksonBodyCodec`.
- Added structured framework error responses with JSON payloads (`code`, `message`).

### Quality

- Added unit and integration coverage for routing, binding, conversion, invocation, transport, and optional JSON handling.
- Added JaCoCo reporting to track line/instruction/branch coverage.

### Known limits

- Controller registration is explicit only (no package scanning).
- No DI container integration.
- Content negotiation is intentionally minimal for this release.
