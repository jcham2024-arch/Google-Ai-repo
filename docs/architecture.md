# System Architecture

Three-module Gradle project. Dependencies point one way only: `:app` → `:security-interface` →
`:core-engine`. CI enforces the direction (guardrail C3 in `.github/workflows/ci.yml`).

| Module | Kind | Language / toolchain | Depends on |
| :--- | :--- | :--- | :--- |
| `:core-engine` | `java-library` | Java 11, no runtime dependencies | — |
| `:security-interface` | `com.android.library` | Java 11 (`minSdk 24`) | `:core-engine`, `androidx.core.ktx` |
| `:app` | `com.android.application` | Kotlin 2.2.10 + Compose (`minSdk 24`, `compileSdk 36`) | both modules, Room, DataStore, WorkManager, OkHttp, Firebase, Retrofit/Moshi |

---

## 1. `:core-engine` — headless reasoning core

Packages under `core-engine/src/main/java/com/apex/core/`:

- `port/` — 14 interfaces: `ApprovalPort`, `AuditPort`, `AuditStorePort`, `ChatModelPort`,
  `ChunkConsumer` (callback interface), `HttpTransportPort`, `JsonCodecPort`, `PlannerPort`,
  `RateLimiterPort`, `SecretsPort`, `StoragePort`, `TelemetryPort`, `ToolExecutorPort`,
  `VectorStorePort`; plus helpers `RetryPolicy`, `NoOpTelemetry`, `SafeTelemetryDecorator`.
- `port/model/` — 14 immutable classes: `ApprovalDecision`, `ApprovalRequest`, `AuditRecord`,
  `ChatDelta`, `ChatRequest`, `ChatResponse`, `ExecutionResult`, `HttpChunk`, `HttpRequest`,
  `HttpResponse`, `Plan`, `PlanStep`, `ToolCall`, `ToolSchema`.
- `client/OpenAiCompatibleClient.java` — chat completions and streaming against any OpenAI-compatible
  base URL, driven through `HttpTransportPort`, not the JDK HTTP client.
- `json/MiniJson.java` — small dependency-free JSON reader/writer used inside the engine.
- `provider/` — `ProviderConfig`, `ProviderConfigurationManager`, `ProviderEngineContext`,
  `ProviderRegistry`.
- `tool/` — `ToolRegistry` and the `ToolWorker` contract implemented by `:app`.

Constraints: `sourceCompatibility`/`targetCompatibility` are Java 11; records, pattern matching and
`Thread.ofVirtual` are prohibited; `java.net.http` is not allowed (guardrail C6 covers the app side,
the engine simply has no such usage). No `android.*` imports. No runtime dependencies.

## 2. `:security-interface` — interlock, policy, audit

- `policy/RiskLevel.java` — `L0…L4`; `requiresApproval()` is true for `L3` and `L4`.
- `policy/ToolRiskRegistry.java` — `ConcurrentHashMap` of tool name → risk level, seeded with
  `read_file` L2, `list_files` L2, `write_file` L3, `write_file_external` L3, `http_get` L2,
  `http_post` L3, `run_command` L4; unknown and null tool names resolve to L3.
- `policy/SystemStateManager.java` — system state used by the interlock to decide whether to fail
  closed.
- `policy/BudgetExhaustedException.java` — thrown when the interlock's iteration budget is spent.
- `interlock/SecurityInterlock.java` — implements `ToolExecutorPort`. Order of operations: risk
  lookup → approval decision (`ApprovalPort`) → delegate execution → audit append. Throws or fails
  closed on a null call, a null/degraded approval port, a mismatched approval request id, a thrown
  delegate exception, or an exhausted budget.
- `interlock/ToolInvocationInterceptor.java` — implements `ToolExecutorPort` as a telemetry decorator:
  opens a `tool.invoke` span with a `tool.id` attribute, counts successes/failures, records
  exceptions, and wraps the delegate telemetry in `SafeTelemetryDecorator`.
- `audit/AuditLogManager.java` — builds the hash chain and verifies it; `FileAuditStore.java`
  persists JSONL records with rotation and force-flush; `AuditChainBrokenException.java` reports the
  first corrupt index.

## 3. `:app` — Android application

Layered packages under `app/src/main/java/com/example/`:

| Package | Contents |
| :--- | :--- |
| `data/adapter` | `OkHttpTransport` (`HttpTransportPort`), `OrgJsonCodec` (`JsonCodecPort`), `OtelTelemetry` (`TelemetryPort`, Logcat-backed), `TokenBucketRateLimiter` (`RateLimiterPort`, unwired) |
| `data/local` | `AppDatabase` (Room, exposes `ApexDao`), `OfflineQueueDao` |
| `data/model` | Room entities: `AgentCard`, `ChatMessage`, `PendingActionEntity`, plus workspace/version models |
| `data/provider` | `AndroidKeyStoreManager` (`SecretsPort` adapter), `DataStoreStorageAdapter` (`StoragePort`), `ProviderConfigurationManager` (DataStore persistence, connection test), `ProviderPresets` |
| `domain/engine` | `AgentOrchestrator`, `AndroidApprovalPort` (`ApprovalPort`), `DiffUtil`, `FileGovernanceEngine`, `ProviderRegistry`, `WorkspaceExporter` |
| `domain/gateway` | `AIGateway` interface and `UniversalAIGatewayImpl` (Gemini and OpenAI-compatible paths, embeddings) |
| `domain/repository` | `ChatRepository` interface and `ChatRepositoryImpl` |
| `domain/sync` | `AgentTaskWorker` (WorkManager worker), `OfflineSyncManager` |
| `domain/tools` | 10 `ToolWorker` implementations |
| `domain/usecase` | `ApproveToolUseCase`, `LoadSettingsUseCase`, `SendMessageUseCase` |
| `ui` | Compose screens: `chat/` (`ChatScreen`, `ChatViewModel`, `ChatUiState`), `conversations/`, `agents/`, `workspace/`, `settings/`, `navigation/AppNavigation`, `theme/` |

### Chat turn flow (as written)

1. `ChatScreen` collects state from `ChatViewModel`.
2. `ChatViewModel` enqueues an `AgentTaskWorker` request through WorkManager and observes its
   progress, cancelling the work by id on stop.
3. `AgentTaskWorker` persists the user message, checks offline state, and asks the repository to run
   the turn. The worker currently calls repository members that are not declared (see
   [status.md](status.md)).
4. `ChatRepositoryImpl` delegates to `AgentOrchestrator.orchestrateTurn(...)`.
5. `AgentOrchestrator` builds the function schema from registered `ToolWorker`s and calls the
   gateway (`UniversalAIGatewayImpl`), which selects the Gemini path or the OpenAI-compatible path
   based on the active `ProviderSettings` from `ProviderRegistry`.
6. Provider settings come from DataStore through `ProviderConfigurationManager`; the API key is
   decrypted from the Android Keystore before each request.

### Tool execution flow (as written)

1. The model returns a tool call; `AgentOrchestrator` resolves the worker through `ToolRegistry`.
2. `SecurityInterlock` classifies the call with `ToolRiskRegistry`, requests approval through
   `AndroidApprovalPort` when `RiskLevel.requiresApproval()` is true, then executes the worker.
3. Each invocation appends an `AuditRecord` to the chain and emits telemetry through
   `ToolInvocationInterceptor`.
4. `ChatScreen` constructs `ToolRegistry`, `AuditLogManager`, `SecurityInterlock` and
   `AndroidApprovalPort` inside a composable, so the interlock exists only while that screen is
   composed. There is no application-level dependency-injection container.

### Deliberate deviations from ideal layering

- Interlock and audit objects are assembled in the UI layer (`ChatScreen.kt`), not in a service or
  DI graph.
- `ChatRepositoryImpl` depends on the concrete `AgentOrchestrator` and `UniversalAIGatewayImpl`
  rather than on `AIGateway`, even though the interface exists.
- Storage adapters exist for both Room and DataStore with partially overlapping responsibilities
  (`FileGovernanceEngine` writes workspace versions through Room, `WorkspaceExporter` exports files).

## 4. Guardrails that constrain the architecture

Enforced in CI: no runtime dependencies in `:core-engine` (C1), no `android.*` imports in
`:core-engine` (C2), one-way layer direction (C3), no `MiniJson` usage outside `:core-engine` (C4),
secret scanning (C5), no direct `HttpURLConnection` in production app code (C6), no raw print calls
(C8), no generator remnants (C10). C7 and C9 are conventions implemented in code
(`ToolInvocationInterceptor`, `SafeTelemetryDecorator`) without a dedicated CI step. See
[ci.md](ci.md) for the exact commands and [status.md](status.md) for what is not yet true.
