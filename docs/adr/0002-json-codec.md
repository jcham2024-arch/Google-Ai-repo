# ADR 0002: Abstract JSON Parsing Behind JsonCodecPort

## Status
Accepted

## Context
The `:core-engine` module is governed by Architectural Guardrail C1, which mandates **zero runtime external dependencies** (`java.base` only). However, the engine requires parsing and serializing JSON payloads for LLM chat interactions, tool calling specifications, and structured outputs.

Directly bundling third-party libraries (e.g. Jackson, Moshi, Gson) into `:core-engine` would violate Guardrail C1 and inflate the core artifact. Conversely, using full Android SDK classes (e.g. `org.json.JSONObject`) inside `:core-engine` would violate Guardrail C2 (zero Android framework pollution in core).

## Decision
1. Define a pure Java 11 `JsonCodecPort` interface in `:core-engine` under `com.apex.core.port`.
2. Provide a minimal fallback implementation (`MiniJson`) inside `:core-engine` for bootstrap formatting and simple field extraction without external dependencies.
3. In `:app`, implement `OrgJsonCodec` utilizing the Android SDK's built-in `org.json` library.
4. Pass the `JsonCodecPort` instance to clients (such as `OpenAiCompatibleClient`) and engine orchestrators via dependency injection at application startup.

## Consequences
### Positive
- Strict isolation of `:core-engine`: remains 100% portable, pure Java 11 with no third-party dependencies.
- Zero binary footprint overhead: utilizes the `org.json` implementation already baked into Android ART.
- Testability: Unit tests in `:core-engine` can run on standard JVMs without mocking Android framework classes.
- Future extensibility: High-performance parsers (e.g., Jackson or Moshi) can be swapped in without modifying `:core-engine`.

### Negative
- Requires a small adapter class (`OrgJsonCodec`) in `:app`.
- Core engine fallback (`MiniJson`) is intentionally minimal and does not support full AST parsing.
