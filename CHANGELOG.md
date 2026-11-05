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

### Fixed
- `SecurityInterlock` denial paths (`DENIED_TOOLS_PAUSED`, `DENIED_BUDGET_EXHAUSTED`,
  `DENIED_MALFORMED`) pass an argument map to `AuditLogManager.createAndAppend` as its signature
  requires, and the denial reason is recorded inside the hashed argument domain through a new
  `denialArguments` helper. `AuditLogManagerTest` uses the same canonicalizing overload.
- `ReadFileToolWorker` and `ReadFileRangeToolWorker` validate every path through the new
  `WorkspacePathGuard` and share the `filesDir/workspace` root with `FileGovernanceEngine`, closing a
  `..` traversal and a `filesDir`-wide read surface. `FileGovernanceEngine.deleteFile` now deletes the
  validated target instead of `File(filesDir, path)`.
- `RunCommandToolWorker` fails closed with `UnsupportedOperationException` instead of returning
  `"Executed command: ... [Status: OK]"` for a command it never ran; its description and the Master
  Agent system prompt no longer advertise execution.
- Production wires `AuditLogManager(FileAuditStore(filesDir/audit))`, so the SHA-256 audit chain is
  persisted to `audit_active.jsonl` instead of being kept in memory.
- Gradle wrapper committed (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) pinned to
  the 9.3.1 distribution, and `ci.yml` plus `release.yml` now invoke `./gradlew`.
- `release.yml` builds and Gradle-signs `:app:assembleRelease` and `:app:bundleRelease`, requires the
  three signing secrets, verifies both signatures with `apksigner` and `jarsigner`, writes
  `SHA256SUMS.txt`, signs the APK and AAB blobs with cosign, and publishes the release artifacts
  instead of debug APKs. It also fails the job when the tag does not match `versionName`.

- The dead `com.github.gfx:markdown-compose` dependency (GitHub repo deleted, JitPack 401) is replaced
  by `com.mikepenz:multiplatform-markdown-renderer-m3:0.40.0`; the ~90-line `MarkdownStyle` block in
  `ChatScreen` is rewritten for the mikepenz `-m3` API (`markdownColor` / `markdownTypography`).

### Known gaps recorded (not fixed in this change)
- `:app:compileDebugKotlin` fails with 286 Kotlin errors across 18 files, all pre-dating this change
  series (measured, per-file counts and error classes in `docs/status.md` §2).
- `run_command` remains registered in the tool registry and advertised in the model tool schema, so a
  turn can still be spent on a call that always fails.
- `TokenBucketRateLimiter` and `SystemStateManager` are not wired into the transport or the interlock.
- Duplicate `ProviderConfigScreen` remains (`ui/ProviderConfigScreen.kt` and
  `ui/settings/ProviderConfigScreen.kt`).

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
