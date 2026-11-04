# Changelog

All notable changes to Apex Agentic Master Chat are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Documentation synchronised with the current code: `README.md`, `docs/index.md`,
  `docs/architecture.md`, `docs/ci.md`, `docs/testing.md`, `docs/e2e.md`, `docs/networking.md`,
  `docs/observability.md`, `docs/monitoring.md`, `docs/scalability.md`, `docs/audit-format.md`,
  `docs/versioning.md`, `docs/security/threat-model.md`, `docs/security/tool-adapters.md`, all six
  runbooks, and the module READMEs for `:core-engine`, `:security-interface` and `:app`.
- File index rebuilt as a hub (`docs/file_index.md`) plus four module parts under `docs/file_index/`,
  covering all 208 tracked files.
- Counts corrected across the docs: 14 port interfaces with 3 helper types, 14 port models, 10 tool
  workers, 7 provider presets, 5 navigation destinations, 53 unit tests in 15 classes plus 1
  instrumented test.
- Guardrail documentation now matches `.github/workflows/ci.yml`: C1, C2, C3, C4, C6, C8 and C10 are
  enforced steps, C5 runs gitleaks without blocking the build, and C7/C9/C11–C13 are conventions
  with no CI step.
- Provider preset list corrected: LM Studio is not a preset; the shipped presets are Gemini
  (OpenAI-compatible), OpenRouter, Groq, DeepSeek, OpenAI, NVIDIA NIM and a local Ollama endpoint.
- Risk mapping documented from the implementation: `read_file`/`list_files`/`http_get` L2,
  `write_file`/`write_file_external`/`http_post` L3, `run_command` L4, unknown tools L3.

### Added
- `docs/status.md`: verified implementation status, unresolved references with file and line numbers,
  tooling gaps, and an explicit list of what was not verified.

### Known gaps recorded (not fixed in this change)
- `:app` has unresolved references (`ChatRepository.getInstance`, `getAgentOrchestrator`,
  `OfflineSyncManager.getInstance`, `AgentOrchestrator.executeToolCall` signature, several Compose
  API uses and imports listed in `docs/status.md`).
- `:security-interface` has three `AuditLogManager.createAndAppend` call sites in `SecurityInterlock`
  that pass a `String` where a `Map<String, String>` argument map is expected.
- No Gradle wrapper is committed although CI, the release workflow and `CONTRIBUTING.md` reference
  `./gradlew`.
- Production constructs `AuditLogManager()` without a store, so audit records are in memory only.
- `run_command` executes no process; `TokenBucketRateLimiter` is not wired into the transport.

## [0.1.0] - 2026-11-03

### Added
- Initial multi-module architecture (`:app`, `:security-interface`, `:core-engine`).
- Pure Java 11 engine exploiting the port/adapter boundary: `ChatModelPort`, `PlannerPort`,
  `ToolExecutorPort`, `ApprovalPort`, `StoragePort`, `VectorStorePort`, `SecretsPort`,
  `TelemetryPort`, `AuditPort`, `AuditStorePort`, `HttpTransportPort`, `JsonCodecPort`,
  `RateLimiterPort`, `ChunkConsumer`.
- Provider configuration UI with preset cards, base URL and model editing, hardware-secured API key
  dialog, and live connection diagnostics.
- `OkHttpTransport` adapter with connection pooling, exponential backoff retries and SSE streaming.
- `SafeTelemetryDecorator` fault isolation with sensitive-field redaction.
- `ToolInvocationInterceptor` emitting spans, logs, histograms and counters for tool executions.
- `FileAuditStore` append-only JSONL audit trail with `FileChannel.force(true)` and SHA-256 chaining.
- `JsonCodecPort` abstraction backed by `OrgJsonCodec` in `:app`.
- `RateLimiterPort` token-bucket limiter and `SystemStateManager` operational states
  (`NORMAL`, `DEGRADED`, `SAFE_MODE`).
- ADR 0001 (ports and adapters), ADR 0002 (JsonCodecPort), ADR 0003 (core-engine dependency policy).
- Operational runbooks under `docs/runbooks/` and MockWebServer-based integration tests
  (`AgentPipelineE2ETest.kt`).
