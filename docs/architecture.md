# System Architecture & Code References

Apex Agentic Master Chat is built using strict Object-Oriented Programming principles and Clean Architecture in Kotlin & Jetpack Compose across a clean three-layer multi-module structure:

```
:app  →  :security-interface  →  :core-engine
```

## Module Architecture

### 1. `:core-engine` (Java 11 Library)
- **Zero third-party runtime dependencies**: Pure Java 11.
- **Provider Architecture** (`com.apex.core.provider`):
  - `ProviderConfig`: Immutable model for AI backends (`id`, `name`, `baseUrl`, `apiKey`, `defaultModel`, `active`).
  - `ProviderRegistry`: Central registry holding active providers and dynamically injecting them into orchestrators with listener callbacks.
  - `ProviderConfigurationManager`: Pure Java manager that persists multiple provider configurations via `StoragePort` and `MiniJson`.
- **Nine Port Interfaces** (`com.apex.core.port`):
  - `ChatModelPort`: LLM chat and streaming generation.
  - `PlannerPort`: Planning and dynamic replanning.
  - `ToolExecutorPort`: Tool invocation dispatch.
  - `ApprovalPort`: Human-in-the-loop approval gating.
  - `StoragePort`: Key-value session/step persistence.
  - `VectorStorePort`: Vector embeddings storage and nearest neighbor search.
  - `SecretsPort`: Hardware-backed secret retrieval.
  - `AuditPort`: Tamper-evident append-only audit trail.
  - `TelemetryPort`: Event recording and latency metrics.
- **Supporting Models** (`com.apex.core.port.model`): Adapted from Java 21 records to Java 11 `final` classes (`ChatRequest`, `ChatResponse`, `ToolCall`, `ExecutionResult`, `Plan`, `PlanStep`, `ApprovalRequest`, `ApprovalDecision`, `AuditRecord`).
- **In-process Tool Execution** (`com.apex.core.tool`): `ToolWorker` interface and `ToolRegistry` on thread pool (replacing desktop `ProcessBuilder`).
- **Zero-Dependency Transport** (`com.apex.core.client`, `com.apex.core.json`): `OpenAiCompatibleClient` using `HttpURLConnection` and hand-rolled `MiniJson`.

### 2. `:security-interface` (Android Library)
- **Approval-Interlock Boundary** (`com.apex.security.interlock.SecurityInterlock`): Gating tool execution behind `ApprovalPort`.
- **Risk Level Policy** (`com.apex.security.policy`):
  - `RiskLevel` (`L0`, `L1`, `L2`, `L3`, `L4`) with `requiresApproval()` (L3 and L4 require user approval).
  - `ToolRiskRegistry`: Maps `read_file`/`list_files` (L0), `write_file` (L1), `write_file_external`/`http_get` (L2), `http_post` (L3), `run_command` (L4). Unknown tools default to L3.
- **Audit Port Implementation** (`com.apex.security.audit.AuditLogManager`): Tamper-evident SHA-256 hash-chain verification (`entryHash = SHA-256(timestamp + toolName + riskLevel + decision + argsHash + previousHash)`).

### 3. `:app` (Android Application)
- **DataStore Storage Adapter** (`com.example.data.provider.DataStoreStorageAdapter`):
  - Implements the `:core-engine` `StoragePort` using Android Jetpack DataStore Preferences.
- **DataStore + Android Keystore** (`com.example.data.provider`):
  - `AndroidKeyStoreManager`: Hardware-backed AES-256 GCM encryption for API keys.
  - `ProviderConfigurationManager`: DataStore Preferences for provider settings.
  - `ProviderPresets`: Full preset catalogue including OpenAI, OpenRouter, Groq, DeepSeek, Gemini (OpenAI-compatible), NVIDIA NIM, Ollama, LM Studio, and Custom.
  - Connection testing via `/models` with fallback to ping `/chat/completions`.
- **Dynamic Ingestion** (`com.example.domain.engine`): `ProviderRegistry` and `NarratorOrchestrator` dynamically inject active provider configurations into the runtime chat engine.
- **UI & Presentation** (`com.example.ui`): Compose navigation (`ui/chat`, `ui/settings`, `ui/agents`, `ui/workspace`, `ui/navigation`).
  - **API Key Management**: Dedicated "Add / Change API Key" button and modal on every provider card in the Settings screen with in-place testing and Keystore encryption.
