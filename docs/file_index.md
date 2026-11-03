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
- **[[../core-engine/src/main/java/com/apex/core/port/ChatModelPort.java|ChatModelPort.java]]**: Port abstraction for LLM inference and streaming.
- **[[../core-engine/src/main/java/com/apex/core/port/PlannerPort.java|PlannerPort.java]]**: Port abstraction for goal planning and step replanning.
- **[[../core-engine/src/main/java/com/apex/core/port/ToolExecutorPort.java|ToolExecutorPort.java]]**: Boundary for tool execution.
- **[[../core-engine/src/main/java/com/apex/core/port/ApprovalPort.java|ApprovalPort.java]]**: Human-in-the-loop authorization port.
- **[[../core-engine/src/main/java/com/apex/core/port/StoragePort.java|StoragePort.java]]**: Key-value persistence port.
- **[[../core-engine/src/main/java/com/apex/core/port/VectorStorePort.java|VectorStorePort.java]]**: Vector embeddings and semantic search port.
- **[[../core-engine/src/main/java/com/apex/core/port/SecretsPort.java|SecretsPort.java]]**: Hardware-backed secret retrieval port.
- **[[../core-engine/src/main/java/com/apex/core/port/AuditPort.java|AuditPort.java]]**: Tamper-evident cryptographic audit log port.
- **[[../core-engine/src/main/java/com/apex/core/port/AuditStorePort.java|AuditStorePort.java]]**: Persistent storage boundary for audit trail verification.
- **[[../core-engine/src/main/java/com/apex/core/port/JsonCodecPort.java|JsonCodecPort.java]]**: Port abstraction for pure JSON encoding/decoding.
- **[[../core-engine/src/main/java/com/apex/core/port/TelemetryPort.java|TelemetryPort.java]]**: Telemetry metrics and latency tracking port.

### Port Models (`com.apex.core.port.model`)
- **[[../core-engine/src/main/java/com/apex/core/port/model/ChatRequest.java|ChatRequest.java]]**: Model representing chat prompt and parameters.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ChatResponse.java|ChatResponse.java]]**: Model representing generated output and tool calls.
- **[[../core-engine/src/main/java/com/apex/core/port/model/ToolCall.java|ToolCall.java]]**: Model representing individual tool call requests.
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
- **[[../core-engine/src/main/java/com/apex/core/client/OpenAiCompatibleClient.java|OpenAiCompatibleClient.java]]**: Pure `HttpURLConnection` client for OpenAI-compatible endpoints with `JsonCodecPort` support.
- **[[../core-engine/src/main/java/com/apex/core/json/MiniJson.java|MiniJson.java]]**: Lightweight zero-dependency fallback JSON codec implementing `JsonCodecPort`.

### Core Engine Tests (`core-engine/src/test/java`)
- **[[../core-engine/src/test/java/com/apex/core/json/MiniJsonTest.java|MiniJsonTest.java]]**: Characterization tests for JSON formatting, escaping, and extraction.
- **[[../core-engine/src/test/java/com/apex/core/port/model/ChatModelPortModelTest.java|ChatModelPortModelTest.java]]**: Port models immutability, defaults, and equals/hashCode contracts.
- **[[../core-engine/src/test/java/com/apex/core/port/ApprovalPortContractTest.java|ApprovalPortContractTest.java]]**: Approval lifecycle contract test suite.

---

## 3. Module: `:security-interface` (Android Library)
- **[[../security-interface/README.md|security-interface/README.md]]**: Technical overview of security policies, interlock, and audit trails.
- **[[../security-interface/build.gradle.kts|security-interface/build.gradle.kts]]**: Android library Gradle build script.
- **[[../security-interface/src/main/java/com/apex/security/policy/RiskLevel.java|RiskLevel.java]]**: RiskLevel enum (`L0`, `L1`, `L2`, `L3`, `L4`).
- **[[../security-interface/src/main/java/com/apex/security/policy/ToolRiskRegistry.java|ToolRiskRegistry.java]]**: Tool risk level mapper with `L3` unknown tool default.
- **[[../security-interface/src/main/java/com/apex/security/audit/AuditChainBrokenException.java|AuditChainBrokenException.java]]**: Exception raised when audit hash chain breaks.
- **[[../security-interface/src/main/java/com/apex/security/audit/FileAuditStore.java|FileAuditStore.java]]**: Durable `.jsonl` append-only audit store with `FileChannel.force(true)` and verification.
- **[[../security-interface/src/main/java/com/apex/security/audit/AuditLogManager.java|AuditLogManager.java]]**: Tamper-evident SHA-256 hash-chained implementation of `AuditPort`.
- **[[../security-interface/src/main/java/com/apex/security/interlock/SecurityInterlock.java|SecurityInterlock.java]]**: Approval-interlock boundary gating tool execution.

### Security Interface Tests (`security-interface/src/test/java`)
- **[[../security-interface/src/test/java/com/apex/security/policy/SecurityInterlockTest.java|SecurityInterlockTest.java]]**: Risk level gating, fail-closed handling, and approval delegation.
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

### Domain & Engine (`com.example.domain`)
- **[[../app/src/main/java/com/example/domain/engine/ProviderRegistry.kt|ProviderRegistry.kt]]**: In-memory reactive provider state holder.
- **[[../app/src/main/java/com/example/domain/engine/NarratorOrchestrator.kt|NarratorOrchestrator.kt]]**: Master Narrator coordinator enforcing file governance.
- **[[../app/src/main/java/com/example/domain/engine/FileGovernanceEngine.kt|FileGovernanceEngine.kt]]**: Workspace file manager and approved self-edit applicator.
- **[[../app/src/main/java/com/example/domain/gateway/AIGateway.kt|AIGateway.kt]]**: Universal multi-provider gateway supporting Gemini and OpenAI endpoints.

### UI Screens (`com.example.ui`)
- **[[../app/src/main/java/com/example/ui/navigation/AppNavigation.kt|AppNavigation.kt]]**: Type-safe navigation bar and destination host.
- **[[../app/src/main/java/com/example/ui/chat/ChatScreen.kt|ChatScreen.kt]]**: Primary chat screen with Narrator refresh and approval dialogs.
- **[[../app/src/main/java/com/example/ui/chat/PerchanceBrowserScreen.kt|PerchanceBrowserScreen.kt]]**: WebView wrapper for Perchance AI Chat Generator with external browser support.
- **[[../app/src/main/java/com/example/ui/settings/SettingsScreen.kt|SettingsScreen.kt]]**: Provider configuration manager with live connection diagnostics.
- **[[../app/src/main/java/com/example/ui/agents/AgentListScreen.kt|AgentListScreen.kt]]**: Specialized agent prompt card manager.
- **[[../app/src/main/java/com/example/ui/workspace/WorkspaceScreen.kt|WorkspaceScreen.kt]]**: Local workspace code and output explorer.

---

## 5. CI Automation & Architecture Documentation
- **[[../.github/workflows/ci.yml|.github/workflows/ci.yml]]**: Automated CI workflow with guardrails C1–C5.
- **[[adr/0002-json-codec.md|docs/adr/0002-json-codec.md]]**: ADR 0002 on `JsonCodecPort` and native `org.json` usage.
- **[[audit-format.md|docs/audit-format.md]]**: Tamper-evident audit format specification and SHA-256 formulas.
- **[[testing.md|docs/testing.md]]**: Testing strategy and test suite documentation.
- **[[ci.md|docs/ci.md]]**: CI pipeline specification and architectural guardrail enforcement.

