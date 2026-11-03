# Contributing to Apex Agent Management Chat

Thank you for contributing! This document defines the development environment setup, architectural guardrails, and pull request workflow.

## 1. Local Setup
- **JDK**: Java 17 (Temurin recommended).
- **Gradle**: Execute tests via `./gradlew test`.
- **E2E Integration Harness**: Execute via `./gradlew :app:testDebugUnitTest`.

## 2. Architectural Guardrails (C1–C13)
All contributions must comply with active architectural guardrails:
- **C1**: Dependencies in `:core-engine` must be listed in `core-engine/allowed-deps.txt` (`ADR 0003`).
- **C2**: `:core-engine` must have zero Android framework imports (`android.*`).
- **C3**: Layer dependency direction is strictly `:app` $\rightarrow$ `:security-interface` $\rightarrow$ `:core-engine`.
- **C4**: Every new public type must have unit test coverage.
- **C5**: No unencrypted API keys or secrets committed to source control.
- **C6**: Outbound HTTP requests must route through `HttpTransportPort`.
- **C7**: Tool executions must emit telemetry signals via `ToolInvocationInterceptor`.
- **C8**: No raw `println`, `printStackTrace`, or `System.out` calls in production source code.
- **C9**: Telemetry failures must never crash execution (`SafeTelemetryDecorator`).

## 3. Pull Request Checklist
1. Update `CHANGELOG.md` under `[Unreleased]`.
2. Ensure all unit and E2E integration tests pass locally.
3. Update relevant documentation in `docs/` for architectural changes.
