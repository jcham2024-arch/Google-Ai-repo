# Apex Agentic Master Chat

Apex Agentic Master Chat is a production-grade, offline-first multi-agent orchestration platform built with a native three-layer multi-module architecture: a pure Java 11 core engine, an Android security interlock relay library, and a modern Jetpack Compose application.

```
┌────────────────────────────────────────────────────────┐
│                         :app                           │
│   (Jetpack Compose GUI • DataStore • Android Keystore) │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
                            ▼
┌────────────────────────────────────────────────────────┐
│                  :security-interface                   │
│ (Approval Gate • Risk Policies • Tamper-Evident Audit) │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
                            ▼
┌────────────────────────────────────────────────────────┐
│                      :core-engine                      │
│     (Nine Port Interfaces • Pure Java 11 • No Deps)    │
└────────────────────────────────────────────────────────┘
```

---

## Complete File Index & Directory Tree

```
apex-android/
├── README.md                                # Root comprehensive system architecture & file index
├── settings.gradle.kts                      # Gradle multi-module configuration (:app, :core-engine, :security-interface)
├── build.gradle.kts                         # Root Gradle build script with plugin declarations
├── metadata.json                            # AI Studio platform application metadata
│
├── core-engine/                             # Pure Java 11 headless engine module (zero third-party dependencies)
│   ├── README.md                            # Detailed documentation for core engine and port interfaces
│   ├── build.gradle.kts                     # Java-library plugin configuration (Java 11 source/target)
│   └── src/main/java/com/apex/core/
│       ├── port/                            # The Nine Architectural Port Interfaces
│       │   ├── ChatModelPort.java           # LLM chat generation and streaming abstraction
│       │   ├── PlannerPort.java             # Task planning and dynamic replanning
│       │   ├── ToolExecutorPort.java        # Tool invocation execution boundary
│       │   ├── ApprovalPort.java            # Human-in-the-loop approval gate port
│       │   ├── StoragePort.java             # Key-value storage abstraction
│       │   ├── VectorStorePort.java         # Vector embedding storage and semantic search port
│       │   ├── SecretsPort.java             # Hardware-backed secret retrieval abstraction
│       │   ├── AuditPort.java               # Tamper-evident append-only audit trail port
│       │   └── TelemetryPort.java           # Telemetry metrics and tracing port
│       │
│       ├── port/model/                      # Port data models (Java 11 final classes, no records)
│       │   ├── ChatRequest.java             # Input prompt, system instructions, and target model
│       │   ├── ChatResponse.java            # LLM output content, status, and tool calls
│       │   ├── ToolCall.java                # Tool identifier, name, and string arguments map
│       │   ├── ExecutionResult.java         # Tool execution result, output, and error messages
│       │   ├── Plan.java                    # High-level goal and sequence of plan steps
│       │   ├── PlanStep.java                # Individual plan step with completion status
│       │   ├── ApprovalRequest.java         # Human approval request with risk level and arguments
│       │   ├── ApprovalDecision.java        # User decision (approved/denied) and comment
│       │   └── AuditRecord.java             # Hash-chained record with SHA-256 entry hashes
│       │
│       ├── tool/                            # In-process tool execution engine
│       │   ├── ToolWorker.java              # In-process tool execution interface
│       │   └── ToolRegistry.java            # Thread-safe tool worker registry on thread pool
│       │
│       ├── client/                          # Lightweight HTTP transport
│       │   └── OpenAiCompatibleClient.java  # Pure HttpURLConnection client for /chat/completions
│       │
│       └── json/                            # Hand-rolled zero-dependency JSON utility
│           └── MiniJson.java                # Lightweight JSON serialization and extraction
│
├── security-interface/                      # Android security library module
│   ├── README.md                            # Detailed documentation for security policies and audit trail
│   ├── build.gradle.kts                     # Android library build script (namespace com.apex.security)
│   └── src/main/java/com/apex/security/
│       ├── policy/                          # Tool risk policy classification
│       │   ├── RiskLevel.java               # Risk levels L0–L4 with requiresApproval() definition
│       │   └── ToolRiskRegistry.java        # Maps tools to risk levels (unknown defaults to L3)
│       │
│       ├── audit/                           # Tamper-evident cryptographic audit log
│       │   └── AuditLogManager.java         # SHA-256 hash-chained implementation of AuditPort
│       │
│       └── interlock/                       # Human-in-the-loop approval boundary
│           └── SecurityInterlock.java       # ToolExecutorPort wrapper gating execution via ApprovalPort
│
├── app/                                     # Android Jetpack Compose GUI module
│   ├── README.md                            # Detailed documentation for Compose screens and state
│   ├── build.gradle.kts                     # App module configuration with multi-module dependencies
│   └── src/main/
│       ├── AndroidManifest.xml              # Manifest with network permissions
│       │
│       ├── java/com/example/
│       │   ├── MainActivity.kt              # App entry point initializing edge-to-edge Compose UI
│       │   │
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   └── Models.kt            # Room entities (AgentCard, ChatMessage, WorkspaceFile, etc.)
│       │   │   ├── local/
│       │   │   │   └── AppDatabase.kt       # Room SQLite database and DAO definitions
│       │   │   └── provider/
│       │   │       ├── AndroidKeyStoreManager.kt       # Hardware-backed AES-256 GCM key encryption
│       │   │       ├── ProviderPresets.kt              # Reconciled preset list (DeepSeek, Groq, Gemini, etc.)
│       │   │       └── ProviderConfigurationManager.kt # DataStore preferences manager with updateModelId & maxTokens
│       │   │
│       │   ├── domain/
│       │   │   ├── engine/
│       │   │   │   ├── ProviderRegistry.kt    # In-memory reactive registry holding active provider StateFlows
│       │   │   │   ├── AgentOrchestrator.kt   # Master Agent orchestrator coordinating turns and tool execution
│       │   │   │   └── FileGovernanceEngine.kt# Secure workspace file writer and approved edit applicator
│       │   │   └── gateway/
│       │   │       └── AIGateway.kt           # UniversalAIGatewayImpl supporting Gemini and OpenAI endpoints
│       │   │
│       │   └── ui/
│       │       ├── navigation/
│       │       │   └── AppNavigation.kt       # Type-safe bottom navigation scaffold
│       │       ├── chat/
│       │       │   └── ChatScreen.kt          # Chat screen with Agent execution trace and approval prompts
│       │       ├── settings/
│       │       │   └── SettingsScreen.kt      # Provider configuration screen with live connection testing
│       │       ├── agents/
│       │       │   └── AgentListScreen.kt     # Agent Swarm card registry and creation dialog
│       │       ├── workspace/
│       │       │   └── WorkspaceScreen.kt     # Workspace file explorer and viewer
│       │       └── ProviderConfigScreen.kt    # Provider configuration with editable Model IDs & masked keys
│       │
│       └── res/
│           ├── drawable/                      # Vector assets and custom launcher icons
│           └── values/
│               └── strings.xml                # Localized application strings (Apex Agent)
│
└── docs/                                    # Obsidian-compatible wiki documentation
    ├── index.md                             # Wiki home index
    ├── architecture.md                      # System architecture and code cross-references
    └── file_index.md                        # Exhaustive source file registry
```

---

## Module Breakdown & Non-Negotiable Constraints

### 1. `:core-engine`
- **Language Level**: Strictly Java 11. Records are prohibited because Android ART API 26+ does not support Java 21 bytecode. All models are implemented as immutable `final` classes.
- **Dependencies**: Exactly zero third-party libraries. Classpath only contains `java.base`.
- **No Desktop APIs**: Desktop-specific classes like `Thread.ofVirtual()`, `java.net.http.HttpClient`, `ProcessBuilder`, and `ProcessHandle` have been replaced with Android-compatible primitives.
- **Nine Port Interfaces**: Clean boundary separating reasoning, planning, tool execution, human approval, persistence, secrets, and auditing.

### 2. `:security-interface`
- **Approval Interlock**: Every tool call passes through `SecurityInterlock`.
- **Risk Level Hierarchy**:
  - `L0`: Non-mutating read actions (`read_file`, `list_files`) - Allowed automatically.
  - `L1`: Local sandboxed writes (`write_file` inside app directory) - Allowed automatically.
  - `L2`: Non-sensitive network or external file reads (`http_get`) - Allowed automatically.
  - `L3`: Network mutations (`http_post` to arbitrary hosts) - **Requires User Approval**.
  - `L4`: Shell execution or root operations (`run_command`) - **Mandatory User Approval**.
- **Cryptographic Audit Trail**: Each tool invocation creates an `AuditRecord` with a SHA-256 hash forming a tamper-evident chain.

### 3. `:app`
- **Zero Direct Engine Access**: The application UI consumes `:security-interface` and never imports `com.apex.core` engine internals directly.
- **Hardware-Backed Keystore**: API keys are encrypted with an AES-256 GCM key stored inside Android Keystore (`AndroidKeyStoreManager`).
- **DataStore Preferences**: Non-sensitive provider configurations (`id`, `name`, `baseUrl`, `defaultModel`, `maxTokens`, `isActive`) are persisted using Jetpack DataStore (`ProviderConfigurationManager`).
- **Dynamic Provider Injection**: `ProviderRegistry` exposes reactive `StateFlow` streams that dynamically inject provider settings into `AgentOrchestrator` on the fly.
- **Connection Diagnostics**: The Settings / ProviderConfig screen includes an integrated "Test Connection" tool.

---

## Supported AI Provider Presets

| Provider Name | Base URL | Default Model | Max Tokens | Key Storage |
| :--- | :--- | :--- | :--- | :--- |
| **Gemini (OpenAI Compatible)** | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash` | 65,536 | Android Keystore |
| **OpenRouter** | `https://openrouter.ai/api/v1` | `meta-llama/llama-3.3-70b-instruct` | 8,192 | Android Keystore |
| **Groq** | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` | 8,192 | Android Keystore |
| **DeepSeek** | `https://api.deepseek.com/v1` | `deepseek-chat` | 8,192 | Android Keystore |
| **OpenAI** | `https://api.openai.com/v1` | `gpt-4o` | 4,096 | Android Keystore |
| **NVIDIA NIM** | `https://integrate.api.nvidia.com/v1` | `meta/llama-3.3-70b-instruct` | 4,096 | Android Keystore |
| **Ollama (Local)** | `http://10.0.2.2:11434/v1` | `llama3` | 4,096 | Optional / Local |
| **Custom** | User configurable | User configurable | User configurable | Android Keystore |

---

## Build & Verification

To verify and compile the complete multi-module project:

```bash
# Build all modules
gradle assembleDebug

# Verify zero third-party dependencies in core engine
gradle :core-engine:dependencies --configuration runtimeClasspath

# Verify no forbidden desktop APIs in core-engine
grep -rn "Thread.ofVirtual\|java.net.http\|ProcessHandle\|ProcessBuilder" core-engine/src

# Verify no Java records in core-engine (Java 11 rule)
grep -rn "record " core-engine/src
```
