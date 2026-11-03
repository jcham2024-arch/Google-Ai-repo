# `:core-engine` — Pure Java 11 Headless Engine

The `:core-engine` module is a zero-dependency, headless Java library containing the agentic reasoning abstractions, tool execution registry, and the **Nine Port Interfaces**.

---

## Architectural Principles

1. **Zero External Dependencies**: The runtime classpath consists strictly of `java.base`. No Jackson, no Gson, no OkHttp, no Retrofit, and no Android framework APIs.
2. **Java 11 Bytecode**: Targeted specifically at Java 11 (`sourceCompatibility = JavaVersion.VERSION_11`). Java 16+ records, pattern matching, and virtual threads (`Thread.ofVirtual`) are prohibited to guarantee compatibility with Android ART on API 24+.
3. **Hexagonal Architecture (Ports and Adapters)**: High-level agent orchestration depends solely on interfaces defined in `com.apex.core.port`.

---

## The Nine Port Interfaces

| Port Interface | Description | Target Implementation |
| :--- | :--- | :--- |
| `ChatModelPort` | Abstraction for LLM completion and streaming. | `OpenAiCompatibleClient` |
| `PlannerPort` | Creates and replans hierarchical goals and execution steps. | In-process ReAct planner |
| `ToolExecutorPort` | Boundary for dispatching tool calls to executable workers. | Gated by `:security-interface` |
| `ApprovalPort` | Human-in-the-loop authorization gate. | UI ModalBottomSheet in `:app` |
| `StoragePort` | Key-value persistence for session contexts and states. | Room SQLite / File store |
| `VectorStorePort` | Vector embedding storage and nearest-neighbor search. | In-memory / SQLite vector |
| `SecretsPort` | Retrieval and storage of sensitive credentials. | Android Keystore |
| `AuditPort` | Cryptographic append-only trail of tool executions. | Hash-chained audit logger |
| `TelemetryPort` | Performance metrics and tracing. | Android Logcat / Diagnostics |

---

## Models & Java 11 Adaptation

To comply with Java 11 constraints on Android, all port models are implemented as immutable `final` classes:

- **`ChatRequest`**: Contains prompt, system instruction, model name, and tool declarations.
- **`ChatResponse`**: Contains generated text, model, status, and detected tool calls.
- **`ToolCall`**: Contains tool ID, tool name, and key-value arguments map.
- **`ExecutionResult`**: Contains execution status, textual output, and error messages.
- **`Plan` & `PlanStep`**: Represents multi-step agent plans and step completion status.
- **`ApprovalRequest` & `ApprovalDecision`**: Encapsulates approval queries, risk level, and user choices.
- **`AuditRecord`**: Immutable record storing timestamps, tool names, risk levels, decisions, and SHA-256 hashes.

---

## In-Process Tool Execution (`com.apex.core.tool`)

Desktop engines frequently rely on `ProcessBuilder` or shell commands to execute tools. On standard Android devices, spawning arbitrary sub-processes is restricted. `:core-engine` replaces process forks with an in-process worker model:

- **`ToolWorker`**: Java interface defining `getName()`, `getDescription()`, and `execute(Map<String, String>)`.
- **`ToolRegistry`**: Thread-safe registry that invokes `ToolWorker` instances concurrently on an `ExecutorService` using cooperative cancellation.

---

## Network Transport & MiniJson

- **`OpenAiCompatibleClient`**: Uses standard `java.net.HttpURLConnection` to dispatch requests to `/chat/completions`. Handles timeouts, authorization headers, and status code verification without third-party HTTP clients.
- **`MiniJson`**: A lightweight, hand-rolled JSON serializer and parser designed for zero-allocation JSON extraction and formatting.
