# Changelog

## Unreleased

- No changes yet.

## v0.1.5 - 2026-06-26

### Build and release

- Test release for validating the new manual release workflow.
- Add a manually triggered GitHub Actions release workflow that derives the release version from `pom.xml`, creates the release tag, and bumps the next `-SNAPSHOT` version.
- Harden release workflows with narrower token permissions, release environment approval, and tag/version validation.

## v0.1.4 - 2026-06-24

### Fixes

- Harden HTTP content handling.

### Build

- Upgrade optional Jackson integration to Jackson 3 using `tools.jackson.core:jackson-databind` 3.2.0.
- Upgrade JUnit Jupiter to 6.1.0.

## v0.1.2 - 2026-04-09

### Build and release

- Add GitHub Actions CI workflow for pushes and pull requests to `development`.
- Add tag-driven GitHub release workflow that uploads built JAR artifacts.
- Align release tag and artifact naming so `v0.1.2` publishes `0.1.2` artifacts.

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
