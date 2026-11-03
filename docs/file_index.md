# Master File Index & Repository Structure

This file provides an index of all files in **Apex Agentic Master Chat** with cross-references to corresponding architectural documentation.

---

## 1. Root Configuration & Documentation
- **[[../README.md|README.md]]**: Root system architecture, three-tier module topology, and build guides.
- **[[../settings.gradle.kts|settings.gradle.kts]]**: Gradle settings declaring multi-module inclusion of `:app`, `:core-engine`, and `:security-interface`.
- **[[../build.gradle.kts|build.gradle.kts]]**: Root build configuration with plugin aliases.
- **[[../metadata.json|metadata.json]]**: AI Studio platform configuration and app metadata.
- **[[../gradle/libs.versions.toml|libs.versions.toml]]**: Version catalog for dependencies and Gradle plugins.

---

## 2. Module: `:core-engine` (Pure Java 11)
- **[[../core-engine/README.md|core-engine/README.md]]**: Technical overview of the pure Java 11 headless engine.
- **[[../core-engine/build.gradle.kts|core-engine/build.gradle.kts]]**: Java library Gradle build script.

### Port Interfaces (`com.apex.core.port`)
- **[[../core-engine/src/main/java/com/apex/core/port/ChatModelPort.java|ChatModelPort.java]]**: Port abstraction for LLM inference and streaming (`streamChat`).
- **[[../core-engine/src/main/java/com/apex/core/port/HttpTransportPort.java|HttpTransportPort.java]]**: Pure Java 11 HTTP transport port boundary.
- **[[../core-engine/src/main/java/com/apex/core/port/ChunkConsumer.java|ChunkConsumer.java]]**: Callback consumer for streaming HTTP chunks.
- **[[../core-engine/src/main/java/com/apex/core/port/RetryPolicy.java|RetryPolicy.java]]**: Exponential backoff retry policy with jitter.
- **[[../core-engine/src/main/java/com/apex/core/port/PlannerPort.java|PlannerPort.java]]**: Port abstraction for goal planning and step replanning.
- **[[../core-engine/src/main/java/com/apex/core/port/ToolExecutorPort.java|ToolExecutorPort.java]]**: Boundary for tool execution.
- **[[../core-engine/src/main/java/com/apex/core/port/ApprovalPort.java|ApprovalPort.java]]**: Human-in-the-loop authorization port.
- **[[../core-engine/src/main/java/com/apex/core/port/StoragePort.java|StoragePort.java]]**: Key-value persistence port.
- **[[../core-engine/src/main/java/com/apex/core/port/VectorStorePort.java|VectorStorePort.java]]**: Vector embeddings and semantic search port.
- **[[../core-engine/src/main/java/com/apex/core/port/SecretsPort.java|SecretsPort.java]]**: Hardware-backed secret retrieval port.
- **[[../core-engine/src/main/java/com/apex/core/port/AuditPort.java|AuditPort.java]]**: Tamper-evident cryptographic audit log port.
- **[[../core-engine/src/main/java/com/apex/core/port/AuditStorePort.java|AuditStorePort.java]]**: Persistent storage boundary for audit trail verification.
- **[[../core-engine/src/main/java/com/apex/core/port/JsonCodecPort.java|JsonCodecPort.java]]**: Port abstraction for pure JSON encoding/decoding.
- **[[../core-engine/src/main/java/com/apex/core/port/TelemetryPort.java|TelemetryPort.java]]**: Telemetry metrics, spans, and structured log port.
- **[[../core-engine/src/main/java/com/apex/core/port/NoOpTelemetry.java|NoOpTelemetry.java]]**: Zero-dependency default fallback telemetry port.
- **[[../core-engine/src/main/java/com/apex/core/port/SafeTelemetryDecorator.java|SafeTelemetryDecorator.java]]**: Fault-isolating wrapper guaranteeing zero telemetry crashes (Constraint C9) and secret redaction.

### Port Models (`com.apex.core.port.model`)
- **[[../core-engine/src/main/java/com/apex/core/port/model/HttpRequest.java|HttpRequest.java]]**: Model representing outbound HTTP request headers and payload.
- **[[../core-engine/src/main/java/com/apex/core/port/model/HttpResponse.java|HttpResponse.java]]**: Model representing HTTP response status and retryable state.
- **[[../core-engine/src/main/java/com/apex/core/port/model/HttpChunk.java|HttpChunk.java]]**: Model representing SSE streaming chunks.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ChatDelta.java|ChatDelta.java]]**: Model representing streaming token deltas.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ChatRequest.java|ChatRequest.java]]**: Model representing chat prompt and parameters.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ChatResponse.java|ChatResponse.java]]**: Model representing generated output and tool calls.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ToolCall.java|ToolCall.java]]**: Model representing individual tool call requests.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ToolSchema.java|ToolSchema.java]]**: Model representing tool parameter validation schemas.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ExecutionResult.java|ExecutionResult.java]]**: Model representing tool execution results.
- **[[../core-engine/src/main/java/com/apex/core/port/model/Plan.java|Plan.java]]**: Model representing an agent plan.
- **[[../core-engine/src/main/java/com/apex/core/port/model/PlanStep.java|PlanStep.java]]**: Model representing plan steps.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ApprovalRequest.java|ApprovalRequest.java]]**: Model representing approval requests.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ApprovalDecision.java|ApprovalDecision.java]]**: Model representing user approval decisions.
- **[[../core-engine/src/main/java/com/apex/core/port/model/AuditRecord.java|AuditRecord.java]]**: Model representing tamper-evident hash-chained audit links.

### In-Process Tool Execution (`com.apex.core.tool`)
- **[[../core-engine/src/main/java/com/apex/core/tool/ToolWorker.java|ToolWorker.java]]**: In-process tool execution interface.
- **[[../core-engine/src/main/java/com/apex/core/tool/ToolRegistry.java|ToolRegistry.java]]**: Concurrent tool worker registry on thread pool.

### Provider Architecture (`com.apex.core.provider`)
- **[[../core-engine/src/main/java/com/apex/core/provider/ProviderConfig.java|ProviderConfig.java]]**: Immutable provider settings model.
- **[[../core-engine/src/main/java/com/apex/core/provider/ProviderRegistry.java|ProviderRegistry.java]]**: Central registry managing active providers with change listeners.
- **[[../core-engine/src/main/java/com/apex/core/provider/ProviderConfigurationManager.java|ProviderConfigurationManager.java]]**: Core manager persisting provider settings via `StoragePort`.

### Client & JSON Utilities (`com.apex.core.client`, `com.apex.core.json`)
- **[[../core-engine/src/main/java/com/apex/core/client/OpenAiCompatibleClient.java|OpenAiCompatibleClient.java]]**: Pure HTTP client for OpenAI-compatible endpoints with `HttpTransportPort` and `streamChat`.
- **[[../core-engine/src/main/java/com/apex/core/json/MiniJson.java|MiniJson.java]]**: Lightweight zero-dependency fallback JSON codec implementing `JsonCodecPort`.

### Core Engine Tests (`core-engine/src/test/java`)
- **[[../core-engine/src/test/java/com/apex/core/json/MiniJsonTest.java|MiniJsonTest.java]]**: Characterization tests for JSON formatting, escaping, and extraction.
- **[[../core-engine/src/test/java/com/apex/core/port/model/ChatModelPortModelTest.java|ChatModelPortModelTest.java]]**: Port models immutability, defaults, and equals/hashCode contracts.
- **[[../core-engine/src/test/java/com/apex/core/port/ApprovalPortContractTest.java|ApprovalPortContractTest.java]]**: Approval lifecycle contract test suite.
- **[[../core-engine/src/test/java/com/apex/core/port/RetryPolicyTest.java|RetryPolicyTest.java]]**: Unit tests verifying exponential backoff calculations and jitter bounds.
- **[[../core-engine/src/test/java/com/apex/core/port/SafeTelemetryDecoratorTest.java|SafeTelemetryDecoratorTest.java]]**: Fault isolation (Constraint C9) and secret redaction tests.

---

## 3. Module: `:security-interface` (Android Library)
- **[[../security-interface/README.md|security-interface/README.md]]**: Technical overview of security policies, interlock, and audit trails.
- **[[../security-interface/build.gradle.kts|security-interface/build.gradle.kts]]**: Android library Gradle build script.
- **[[../security-interface/src/main/java/com/apex/security/policy/RiskLevel.java|RiskLevel.java]]**: RiskLevel enum (`L0`, `L1`, `L2`, `L3`, `L4`).
- **[[../security-interface/src/main/java/com/apex/security/policy/ToolRiskRegistry.java|ToolRiskRegistry.java]]**: Tool risk level mapper with `L3` unknown tool default.
- **[[../security-interface/src/main/java/com/apex/security/policy/BudgetExhaustedException.java|BudgetExhaustedException.java]]**: Exception thrown when tool iteration limits are exceeded.
- **[[../security-interface/src/main/java/com/apex/security/audit/AuditChainBrokenException.java|AuditChainBrokenException.java]]**: Exception raised when audit hash chain breaks.
- **[[../security-interface/src/main/java/com/apex/security/audit/FileAuditStore.java|FileAuditStore.java]]**: Durable `.jsonl` append-only audit store with `FileChannel.force(true)` and verification.
- **[[../security-interface/src/main/java/com/apex/security/audit/AuditLogManager.java|AuditLogManager.java]]**: Tamper-evident SHA-256 hash-chained implementation of `AuditPort`.
- **[[../security-interface/src/main/java/com/apex/security/interlock/SecurityInterlock.java|SecurityInterlock.java]]**: Fail-closed approval-interlock boundary gating tool execution.
- **[[../security-interface/src/main/java/com/apex/security/interlock/ToolInvocationInterceptor.java|ToolInvocationInterceptor.java]]**: Interceptor emitting automated telemetry spans, logs, histograms, and counters.

### Security Interface Tests (`security-interface/src/test/java`)
- **[[../security-interface/src/test/java/com/apex/security/policy/SecurityInterlockTest.java|SecurityInterlockTest.java]]**: Risk level gating, fail-closed handling, and approval delegation.
- **[[../security-interface/src/test/java/com/apex/security/interlock/SecurityInterlockFailClosedTest.java|SecurityInterlockFailClosedTest.java]]**: Comprehensive fail-closed edge case tests.
- **[[../security-interface/src/test/java/com/apex/security/interlock/ToolInvocationInterceptorTest.java|ToolInvocationInterceptorTest.java]]**: Automated metric and span emission tests.
- **[[../security-interface/src/test/java/com/apex/security/audit/AuditLogManagerTest.java|AuditLogManagerTest.java]]**: Hash chain integrity and SHA-256 validation.
- **[[../security-interface/src/test/java/com/apex/security/audit/FileAuditStoreTest.java|FileAuditStoreTest.java]]**: File persistence, fsync forcing, tampering detection, and rotation.

---

## 4. Module: `:app` (Jetpack Compose Application)
- **[[../app/README.md|app/README.md]]**: Technical overview of the Jetpack Compose app module.
- **[[../app/build.gradle.kts|app/build.gradle.kts]]**: App module Gradle build script.
- **[[../app/src/main/java/com/example/MainActivity.kt|MainActivity.kt]]**: Root activity launching edge-to-edge Compose UI.

### Data & Provider Persistence (`com.example.data`)
- **[[../app/src/main/java/com/example/data/model/Models.kt|Models.kt]]**: Room SQLite entities (AgentCard, ChatMessage, WorkspaceFile, Lorebook, ProviderConfig).
- **[[../app/src/main/java/com/example/data/local/AppDatabase.kt|AppDatabase.kt]]**: Room Database and DAO interfaces.
- **[[../app/src/main/java/com/example/data/provider/AndroidKeyStoreManager.kt|AndroidKeyStoreManager.kt]]**: Hardware-backed AES-256 GCM API key encryption.
- **[[../app/src/main/java/com/example/data/provider/ProviderPresets.kt|ProviderPresets.kt]]**: Provider preset definitions including DeepSeek, OpenRouter, Groq, and Ollama.
- **[[../app/src/main/java/com/example/data/provider/ProviderConfigurationManager.kt|ProviderConfigurationManager.kt]]**: DataStore preferences manager with live connection testing.
- **[[../app/src/main/java/com/example/data/provider/DataStoreStorageAdapter.kt|DataStoreStorageAdapter.kt]]**: Adapter bridging core StoragePort to Android DataStore.
- **[[../app/src/main/java/com/example/data/adapter/OrgJsonCodec.kt|OrgJsonCodec.kt]]**: Adapter implementing `JsonCodecPort` via Android's native `org.json`.
- **[[../app/src/main/java/com/example/data/adapter/OkHttpTransport.kt|OkHttpTransport.kt]]**: Production OkHttp implementation of `HttpTransportPort`.
- **[[../app/src/main/java/com/example/data/adapter/OtelTelemetry.kt|OtelTelemetry.kt]]**: Android Log-backed implementation of `TelemetryPort`.
- **[[../app/src/main/java/com/example/ui/settings/ProviderConfigScreen.kt|ProviderConfigScreen.kt]]**: Dedicated Jetpack Compose provider configuration screen with Base URL, API Key, and Model ID editing.

### E2E Integration Tests (`app/src/test/java/com/example/e2e`)
- **[[../app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt|AgentPipelineE2ETest.kt]]**: End-to-end integration tests using MockWebServer and frozen fixtures.

---

## 5. CI Automation & Architecture Documentation
- **[[../.github/workflows/ci.yml|.github/workflows/ci.yml]]**: Automated CI workflow with guardrails C1–C8.
- **[[adr/0002-json-codec.md|docs/adr/0002-json-codec.md]]**: ADR 0002 on `JsonCodecPort` and native `org.json` usage.
- **[[audit-format.md|docs/audit-format.md]]**: Tamper-evident audit format specification and SHA-256 formulas.
- **[[networking.md|docs/networking.md]]**: Networking architecture, timeouts, retry policies, and SSE streaming.
- **[[observability.md|docs/observability.md]]**: Observability, signal taxonomy, and `SafeTelemetryDecorator` fault isolation.
- **[[security/threat-model.md|docs/security/threat-model.md]]**: Threat model, trust boundaries, threats T1–T5, and mitigations.
- **[[security/tool-adapters.md|docs/security/tool-adapters.md]]**: Security checklist for tool adapters.
- **[[e2e.md|docs/e2e.md]]**: End-to-end integration test guide.
- **[[testing.md|docs/testing.md]]**: Testing strategy and test suite documentation.
- **[[ci.md|docs/ci.md]]**: CI pipeline specification and architectural guardrail enforcement.


