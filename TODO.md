# TODO

## Post-v0.1 backlog

- Enforce richer `@Produces`/`@Consumes` negotiation rules
- Add package scanning option for controller registration
- Add interceptor/filter extension points
- Add validation hooks (parameter/body validation integration)
- Improve content negotiation beyond basic content-type checks
- Support `CompletableFuture` handler return types
- Add streaming response support
- Add alternative transport adapter (for example Undertow or Netty)
- Add OpenAPI generation
- Add metrics/tracing hooks

## Quality and ergonomics

- Improve startup error diagnostics (source context and route identifiers)
- Add more integration tests for malformed JSON and codec fallback behavior
- Add benchmark-style smoke checks for routing and binding hot paths

All items in this file are intentionally future-facing and are not required for the `v0.1.x` releases.
