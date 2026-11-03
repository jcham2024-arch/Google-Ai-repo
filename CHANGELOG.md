# Changelog

All notable changes to Apex Agent Management Chat will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Provider Configuration Compose UI (`SettingsScreen.kt`) featuring preset cards (DeepSeek, OpenRouter, Groq, Gemini, OpenAI, NVIDIA NIM, Ollama, LM Studio), Base URL and Model ID editing, hardware-secured API key dialog, and live connection diagnostics.
- `HttpTransportPort` abstraction in `:core-engine` with `OkHttpTransport` adapter in `:app` supporting connection pooling, exponential backoff retries, and SSE streaming.
- `TelemetryPort` abstraction in `:core-engine` with `SafeTelemetryDecorator` fault isolation (Constraint C9) and sensitive field redaction.
- `ToolInvocationInterceptor` emitting automated spans, logs, histograms, and counters for all tool executions.
- `FileAuditStore` append-only `.jsonl` audit trail with `FileChannel.force(true)` and SHA-256 hash chaining.
- `JsonCodecPort` abstraction in `:core-engine` backed by native `OrgJsonCodec` in `:app`.
- `RateLimiterPort` token-bucket rate limiter and `SystemStateManager` operational state management (`Normal`, `Degraded`, `Safe Mode`).
- Architectural Decision Records: `ADR 0001` (Ports & Adapters), `ADR 0002` (JsonCodecPort), `ADR 0003` (Core Engine Dependencies Policy).
- Operational runbooks under `docs/runbooks/` and end-to-end integration tests using MockWebServer (`AgentPipelineE2ETest.kt`).

## [0.1.0] - 2026-11-03

### Added
- Initial release of Apex Agent Management Chat multi-module architecture (`:app`, `:security-interface`, `:core-engine`).
