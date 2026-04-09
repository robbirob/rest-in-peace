# Architecture

## Package overview

- `de.rfr.restinpeace.api.*`: public annotations and API types (`HttpResponse`, `BodyCodec`, `ExceptionMapper`)
- `de.rfr.restinpeace.core.*`: routing, startup inspection, parameter binding, conversion, invocation, and error handling
- `de.rfr.restinpeace.transport.jdk`: thin adapter for JDK `HttpServer`
- `de.rfr.restinpeace.json.jackson`: optional Jackson-based `BodyCodec`
- `de.rfr.restinpeace.example`: runnable sample controllers and bootstrap

## Request lifecycle

1. `RestApp.builder().build()` inspects controllers and builds `RouteRegistry`.
2. `JdkHttpServerAdapter` converts `HttpExchange` into `FrameworkRequest`.
3. `RequestExecutor` resolves route and checks method/path behavior (404/405).
4. `ParameterBinder` binds path/query/header/body values into handler arguments.
5. Handler method is invoked via reflection.
6. Return value is mapped to `HttpResponse` semantics.
7. `@Consumes` and `@Produces` are applied in the invocation layer.
8. Exceptions are mapped through custom mappers first, then default framework mapping.
9. Framework errors are serialized as JSON payloads (`{"code":"...","message":"..."}`).
10. Transport writes status, headers, and encoded body back to the client.

## Route matching strategy

- Routes are keyed by HTTP method plus normalized path template.
- Path templates support variable segments like `/users/{id}`.
- Matching precedence is deterministic: exact segments win over variable segments.
- Ambiguous route patterns for the same method fail at startup.

## Parameter binding strategy

- Supported annotations: `@PathParam`, `@QueryParam`, `@HeaderParam`, `@Body`.
- Unannotated parameters are rejected at startup.
- Supported scalar conversion: `String`, primitives/boxed numerics and booleans, enums, `Optional<T>`.
- Missing value rules:
  - primitive path/query/header -> `400`
  - reference query/header -> `null`
  - `Optional<T>` -> `Optional.empty()`

## Codec strategy

- Core depends on `BodyCodec` only and is independent from Jackson.
- Default body handling in core supports `String`, `byte[]`, and empty body.
- Response defaults: `text/plain; charset=utf-8` for `String`, `application/octet-stream` for `byte[]`, and `application/json` for object responses.
- JSON POJO support is enabled by adding `JacksonBodyCodec`.

## Why JDK HttpServer first

- No mandatory extra runtime dependency.
- Lets the project focus on framework behavior (routing/binding/invocation).
- Transport remains replaceable behind framework request/response abstractions.
