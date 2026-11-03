# ADR 0001: Hexagonal Ports and Adapters Architecture

## Status
Accepted

## Context
Apex requires a modular architecture where the core execution engine (`:core-engine`) remains isolated from platform-specific frameworks (Android SDK, UI libraries, specific database drivers, and network clients).

## Decision
Adopt **Hexagonal Architecture (Ports and Adapters)**:

1. **Ports (`com.apex.core.port`)**: All system boundaries (LLM inference, storage, secrets, audit logs, telemetry, HTTP transport, rate limiting, and approval UI) are declared as pure interfaces in `:core-engine`.
2. **Adapters (`:app`, `:security-interface`)**: Concrete implementations of ports live in external modules and are injected into core components at runtime.
3. **Dependency Rule**: Dependencies flow strictly inward (`:app` $\rightarrow$ `:security-interface` $\rightarrow$ `:core-engine`). `:core-engine` never imports external modules.

## Consequences
### Positive
- Core business logic and safety interlocks are 100% testable on standard JVMs without emulators.
- Platform components (e.g. OkHttp, Android KeyStore, DataStore, OpenTelemetry) can be swapped or upgraded without modifying `:core-engine`.

### Negative
- Requires defining explicit Port interfaces and value models for every external boundary.
