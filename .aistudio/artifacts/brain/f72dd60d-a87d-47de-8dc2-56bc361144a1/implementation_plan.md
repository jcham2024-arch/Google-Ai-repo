# Missing Features Build List & Revised Scope (Agent Architecture)

This revised implementation plan addresses the **Missing Features Build List (Priorities 0 through 3)** incorporating critical scope revisions for `ApexAgentManagementChat` / `apex-android`.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following technical scope revisions were confirmed:

- **Confirmed Scope Change 1 (Removal of Perchance Generator)**: Completely remove the Perchance AI generator module, UI screens (`PerchanceBrowserScreen.kt`), prompt/template generators, navigation routes, and generator test fixtures. Enforce a new CI Guardrail check that fails the build if `perchance` or `generator` identifiers reappear in source code or Gradle module definitions.
- **Confirmed Scope Change 2 (Narrator $\rightarrow$ Agent Repurposing)**: Rename and repurpose `NarratorOrchestrator` to `AgentOrchestrator`. The agent operates as a general-purpose task planner and executor that decomposes user requests into actionable steps, calls `AIGateway`, executes tools through `ToolRegistry` and `SecurityInterlock`, and renders a real-time step-by-step agent trace in `ChatScreen.kt`.
- **Confirmed Decision 3 (App-Layer Clean Architecture)**: Wire `ChatViewModel` and `SettingsViewModel`, backed by `ChatRepository` and `SettingsRepository`, and domain use cases (`SendMessageUseCase`, `ApproveToolUseCase`, `LoadSettingsUseCase`). UI states are exposed via `StateFlow` and consumed in Jetpack Compose via `collectAsStateWithLifecycle()`.
- **Confirmed Decision 4 (Offline-First Queue & Sync - Item 10)**: Implement Room SQLite (`OfflineQueueDao`, `PendingActionEntity`) and an `OfflineSyncManager` that enqueues pending agent tasks and tool actions when offline, replaying them on reconnect while preserving `FileAuditStore` append-only hash trail integrity.
- **Confirmed Decision 5 (Path Validation Strategy)**: Support path access with explicit human approval prompts (`ApprovalPort`) for file operations (`read_file`, `write_file`) and shell commands (`run_command`).

---

## 1. Overview & Core Concept

- **What It Does**: Purges legacy generator code while completing the missing features across Priorities 0–3, establishing Clean Architecture ViewModel/UseCase orchestration for `AgentOrchestrator`, hardware-backed security primitives, multi-provider AI gateway adapters, bounded tool executors, DataStore/Keystore persistence, interactive tool approval UI, and offline-first queue/sync capabilities.
- **Target Audience**: End users interacting with the general-purpose Apex agentic assistant, as well as developers auditing system architecture and test suites.
- **Key Value**: Delivers a streamlined, production-grade Android application where user intent is planned and executed by `AgentOrchestrator` through Clean Architecture ViewModels, security interlocks, multi-provider gateways, and offline sync queues without crash or data loss.

---

## 2. User Experience & Visual Design

- **Chat Screen (`ChatScreen.kt`)**: Displays real-time message history, streaming token deltas, step-by-step agent execution traces, loading indicators, error banners, and interactive tool approval modals.
- **Interactive Tool Approval Modal**: Prompts the user with tool details (`read_file`, `write_file`, `http_post`, `run_command`), parameter values, and risk level (`L0`–`L4`) before executing high-risk operations.
- **Offline Sync Badge**: Displays network connectivity status and pending offline queue item count with auto-sync status indicators.
- **Provider Settings Screen (`SettingsScreen.kt` & `ProviderConfigScreen.kt`)**: Configures provider base URLs, encrypted API keys, model IDs, and connection diagnostics.

---

## 3. Technical Architecture & System Layout

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                    :app                                       │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌─────────────────────┐  │
│  │ ChatScreen / Settings  │ │ ChatViewModel        │ │ SettingsViewModel   │  │
│  │ (Compose UI + States)  │ │ (StateFlow & UseCases)│ │ (StateFlow)         │  │
│  └───────────┬────────────┘ └──────────┬───────────┘ └──────────┬──────────┘  │
│              │                         │                        │             │
│              ▼                         ▼                        ▼             │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │ Repositories: ChatRepository • SettingsRepository • OfflineSyncManager  │  │
│  │ (Room SQLite PendingQueue • DataStore Preferences • KeyStore Manager)   │  │
│  └─────────────────────────────┬───────────────────────────────────────────┘  │
└────────────────────────────────┼──────────────────────────────────────────────┘
                                 │ depends on
                                 ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                             :security-interface                               │
│  ┌──────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────┐ │
│  │ ToolInvocationInterceptor│ │    SecurityInterlock    │ │ FileAuditStore  │ │
│  │ (Emits Spans & Logs)     │ │  (Fail-Closed L0-L4)    │ │ (.jsonl SHA256) │ │
│  └───────────┬──────────────┘ └───────────┬─────────────┘ └────────┬────────┘ │
└──────────────┼────────────────────────────┼────────────────────────┼──────────┘
               │ depends on                 │ depends on             │ depends on
               ▼                        ▼                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                                 :core-engine                                  │
│  ┌───────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐  │
│  │   AgentOrchestrator   │ │   ProviderRegistry   │ │  ToolRegistry &      │  │
│  │ (General Task Planner)│ │ (Dynamic Injection)  │ │  ToolWorkers         │  │
│  └───────────────────────┘ └──────────────────────┘ └──────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Detailed Task Breakdown & Implementation Steps

### Scope Purge & Guardrails
1. **Remove Perchance Generator**:
   - Delete `PerchanceBrowserScreen.kt` and references in `AppNavigation.kt`.
   - Remove generator-related settings keys, prompt templates, and navigation entries.
   - Add CI Guardrail in `.github/workflows/ci.yml` checking that `grep -rn -i "perchance\|generator" app/src/main/` returns 0 hits.
2. **Repurpose Narrator $\rightarrow$ AgentOrchestrator**:
   - Rename `NarratorOrchestrator` to `AgentOrchestrator` in `com.example.domain.engine`.
   - Repurpose orchestrator for general-purpose task decomposition, step planning, tool execution through `ToolRegistry`, and `AIGateway` routing.

### Priority 0 — Compilation & App-Layer Orchestration
3. **Define UI States (`com.example.ui.chat`, `com.example.ui.settings`)**:
   - `ChatUiState` (`messages`, `isGenerating`, `agentSteps`, `pendingToolApproval`, `offlineQueueCount`, `errorMessage`).
   - `SettingsUiState` (`providers`, `activeProvider`, `isTestingConnection`).
   - `ToolApprovalUiState` (`requestId`, `toolName`, `arguments`, `riskLevel`).
4. **Implement Repositories & Use Cases (`com.example.domain`)**:
   - `SendMessageUseCase`: Accepts user prompt, checks network state, routes to `AgentOrchestrator` or queues in `OfflineSyncManager`.
   - `ApproveToolUseCase`: Dispatches user decision (`approve`/`deny`) to `ApprovalPort` and `SecurityInterlock`.
   - `LoadSettingsUseCase`: Loads persisted provider settings via `SettingsRepository`.
   - `ChatRepositoryImpl`: Coordinates message persistence in Room SQLite and agent request execution.
   - `SettingsRepositoryImpl`: Bridges `ProviderConfigurationManager` and `AndroidKeyStoreManager`.
5. **Implement ViewModels (`com.example.ui.chat`, `com.example.ui.settings`)**:
   - `ChatViewModel`: Exposes `StateFlow<ChatUiState>`, handles `sendMessage()`, `approveTool()`, `denyTool()`.
   - `SettingsViewModel`: Exposes `StateFlow<SettingsUiState>`, handles `saveProvider()`, `setActiveProvider()`, `testConnection()`.
   - Connect `ChatScreen.kt` and `SettingsScreen.kt` via `collectAsStateWithLifecycle()`.
6. **Architectural Guardrail Verification**:
   - Verify zero third-party dependencies in `:core-engine` against `allowed-deps.txt`.
   - Verify `gradle assembleDebug` and CI guardrails C1–C8.

### Priority 1 — Core Security & Orchestration
7. **Security Primitives Implementation**:
   - Verify `AuditLogManager` SHA-256 hash chaining and tamper-evident `.jsonl` persistence in `FileAuditStore`.
   - Verify `AndroidKeyStoreManager` AES-256-GCM hardware key encryption.
   - Verify `SecurityInterlock` fail-closed risk policies (`L0`–`L4`) and `ApprovalPort` delegation.
8. **AgentOrchestrator & Registry Wiring**:
   - Verify `AgentOrchestrator` task planning, step decomposition, tool invocation through `ToolRegistry`, and `AIGateway` multi-provider routing.
   - Verify `ProviderRegistry` dynamic injection via `StateFlow`.

### Priority 2 — Provider & Tool Functionality
9. **AI Provider Adapters**:
   - Verify `OpenAiCompatibleClient` supporting Gemini, OpenAI, Groq, DeepSeek, and local Ollama/LM Studio endpoints via `HttpTransportPort`.
10. **Tool Executors & Risk Registry**:
    - Implement `ToolWorker` implementations for `read_file`, `write_file`, `http_get`, `http_post`, and `run_command`.
    - Register risk levels in `ToolRiskRegistry`: `read_file` (L0), `write_file` (L1), `http_get` (L2), `http_post` (L3), `run_command` (L4).
    - Require human approval for L3 and L4 tools.

### Priority 3 — Product Completeness & Offline Sync
11. **Settings Persistence & Injection**:
    - DataStore preferences for non-secret settings; API keys stored in `AndroidKeyStoreManager`.
12. **Chat UI State & Tool Approval Prompts**:
    - Build interactive tool approval dialog and agent step trace view in `ChatScreen.kt`.
13. **End-to-End Integration Test Harness**:
    - Run `AgentPipelineE2ETest.kt` using MockWebServer and frozen fixtures verifying full end-to-end agent flow.
14. **Offline-First Queue & Sync (Item 10)**:
    - Create `PendingActionEntity` and `OfflineQueueDao` in Room SQLite database.
    - Implement `OfflineSyncManager` that intercepts offline agent tasks/tool actions, enqueues them in SQLite, monitors network connectivity, and automatically replays pending actions on reconnect while preserving `FileAuditStore` hash-chain integrity.

---

## 5. Definition of Done Checklist

- [ ] Perchance generator module, UI (`PerchanceBrowserScreen.kt`), and navigation routes completely purged.
- [ ] CI Guardrail checking for `perchance` or `generator` references added and passing.
- [ ] `NarratorOrchestrator` renamed and repurposed to `AgentOrchestrator` for general-purpose task execution.
- [ ] UI states (`ChatUiState`, `SettingsUiState`, `ToolApprovalUiState`) defined.
- [ ] Repositories (`ChatRepository`, `SettingsRepository`) and UseCases (`SendMessageUseCase`, `ApproveToolUseCase`, `LoadSettingsUseCase`) implemented.
- [ ] ViewModels (`ChatViewModel`, `SettingsViewModel`) implemented and wired to Compose screens via `collectAsStateWithLifecycle()`.
- [ ] Security primitives (`AuditLogManager`, `AndroidKeyStoreManager`, `SecurityInterlock`) verified with unit tests.
- [ ] `AgentOrchestrator` and `ProviderRegistry` fully wired and functional.
- [ ] Tool Executors (`read_file`, `write_file`, `http_get`, `http_post`, `run_command`) registered in `ToolRegistry` and `ToolRiskRegistry`.
- [ ] Interactive Tool Approval Modal and Agent Execution Step Trace integrated into `ChatScreen.kt`.
- [ ] Offline-first queue (`PendingActionEntity`, `OfflineQueueDao`) and `OfflineSyncManager` implemented and tested.
- [ ] E2E integration test suite (`AgentPipelineE2ETest.kt`) passing.
- [ ] Full app compilation (`compile_applet`) succeeds.
