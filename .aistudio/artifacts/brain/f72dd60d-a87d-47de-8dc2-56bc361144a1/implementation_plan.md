# Implementation Plan: Full Multi-Module Architecture & Harvest Integration

This plan implements the multi-module Java-native and Android architecture specified in `PROJECT_AGENT_INSTRUCTIONS.md` and the supplementary harvesting guidelines from `ApexAgentManagementChat`.

---

## 1. Architectural Blueprint & Module Hierarchy

A clean three-layer architecture with strictly one-way dependencies:
```
:app  →  :security-interface  →  :core-engine
```

### Module Breakdown:
1. **`:core-engine`** (`java-library`, Java 11):
   - **Zero external dependencies** (runtime classpath contains only `java.base`).
   - Pure Java implementation of the **Nine Port Interfaces** (`ChatModelPort`, `PlannerPort`, `ToolExecutorPort`, `ApprovalPort`, `StoragePort`, `VectorStorePort`, `SecretsPort`, `AuditPort`, `TelemetryPort`).
   - Supporting model types converted from Java 21 records to Java 11 `final` classes.
   - In-process `ToolWorker` interface (replacing desktop `ProcessBuilder`).
   - Lightweight `OpenAiCompatibleClient` using `HttpURLConnection` and hand-rolled JSON parsing.

2. **`:security-interface`** (`com.android.library`):
   - Approval-interlock boundary gating every tool call.
   - `RiskLevel` enum (`L0`, `L1`, `L2`, `L3`, `L4`) with default risk categorization and L3 fallback for unknown tools.
   - `ToolRiskRegistry` mapping tools to risk policies.
   - SHA-256 hash-chained append-only `AuditLog` implementation of `AuditPort`.
   - Interlock Relay SPI surface exposed to `:app`.

3. **`:app`** (`com.android.application`, Kotlin + Compose):
   - Does **not** import `:core-engine` directly; communicates through `:security-interface`.
   - **DataStore + Android Keystore**: Secure API key storage in Android Keystore under unique alias, while non-sensitive provider metadata is stored in DataStore Preferences.
   - **Complete Provider Presets**: OpenAI, OpenRouter, Groq, DeepSeek, Gemini (OpenAI compat), NVIDIA NIM, Ollama (Local), LM Studio (Local), and Custom.
   - **Connection Testing**: `GET {baseUrl}/models` with fallback ping to `POST /chat/completions`.
   - **Jetpack Compose UI**: Settings provider editor, Chat screen with human-in-the-loop approval dialogs, and workspace management.

---

## 2. Proposed Changes & Implementation Steps

### Step 1: Multi-Module Project Configuration
- Update `/settings.gradle.kts` to include `:app`, `:core-engine`, and `:security-interface`.
- Create `/core-engine/build.gradle.kts` with `java-library` plugin and `sourceCompatibility = JavaVersion.VERSION_11`.
- Create `/security-interface/build.gradle.kts` with `com.android.library` plugin depending on `:core-engine`.
- Update `/app/build.gradle.kts` to declare dependency on `project(":security-interface")` and `project(":core-engine")`.

### Step 2: Implement `:core-engine` (Java 11, Pure Java)
- Implement Nine Port Interfaces in package `com.apex.core.port`:
  - `ChatModelPort`, `PlannerPort`, `ToolExecutorPort`, `ApprovalPort`, `StoragePort`, `VectorStorePort`, `SecretsPort`, `AuditPort`, `TelemetryPort`.
- Implement supporting models (`ChatMessage`, `ToolCall`, `ExecutionResult`, `Plan`, etc.) as Java 11 final classes.
- Implement in-process `ToolWorker` and `ToolRegistry`.
- Implement `OpenAiCompatibleClient` and hand-rolled JSON parser (`MiniJson`).

### Step 3: Implement `:security-interface` (Android Library)
- Implement `RiskLevel` enum with `requiresApproval()`.
- Implement `ToolRiskRegistry` with default risk mapping (L0–L4) and unknown tool defaults.
- Implement `AuditEntry` and tamper-evident SHA-256 hash-chained `AuditLogManager`.
- Implement `SecurityInterlock` gating tool execution and firing approval callbacks to the host UI.

### Step 4: Implement Phase A Provider System in `:app`
- Implement `AndroidKeyStoreManager` encrypting/decrypting API keys with hardware-backed AES keys.
- Update `ProviderPresets` to include DeepSeek (`https://api.deepseek.com/v1`).
- Wire `ProviderConfigurationManager` with DataStore + Keystore.
- Enhance Settings UI with connection testing, model picker, and preset selection.
- Connect Chat UI approval dialog to `SecurityInterlock`.

### Step 5: Verification & Compilation
- Run `compile_applet` to verify compilation across all three modules (`:core-engine`, `:security-interface`, `:app`).
- Verify no desktop-only APIs (`Thread.ofVirtual`, `java.net.http`, `ProcessBuilder`) exist in `:core-engine`.
- Verify `:core-engine` has no Android imports.
- Verify `:app` does not import `com.apex.core` engine internals directly.

---

## 3. Definition of Done & Acceptance Criteria

1. Gradle builds successfully with all three modules (`:app`, `:security-interface`, `:core-engine`).
2. `:core-engine` compiles cleanly under Java 11 with zero third-party dependencies.
3. Nine Port Interfaces and `RiskLevel` enum are fully functional.
4. API keys are safely managed with Android Keystore.
5. All provider presets (including DeepSeek) are present and editable.
6. In-app approval flow surfaces tool execution dialogs before critical operations.
