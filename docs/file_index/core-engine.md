# File Index — `:core-engine`

Part of the [master file index](../file_index.md). 48 tracked files.

Module root: [`core-engine/build.gradle.kts`](../../core-engine/build.gradle.kts) (`java-library`,
Java 11, only `testImplementation(libs.junit)`),
[`core-engine/README.md`](../../core-engine/README.md),
[`core-engine/allowed-deps.txt`](../../core-engine/allowed-deps.txt) (dependency allow-list referenced
by ADR 0003 and guardrail C1).

## Port interfaces and helpers — `src/main/java/com/apex/core/port/`

| File | Purpose |
| :--- | :--- |
| [ApprovalPort.java](../../core-engine/src/main/java/com/apex/core/port/ApprovalPort.java) | Human-in-the-loop authorization gate for risky tool calls |
| [AuditPort.java](../../core-engine/src/main/java/com/apex/core/port/AuditPort.java) | Sink for audit records |
| [AuditStorePort.java](../../core-engine/src/main/java/com/apex/core/port/AuditStorePort.java) | Read/verify surface over the persisted audit log |
| [ChatModelPort.java](../../core-engine/src/main/java/com/apex/core/port/ChatModelPort.java) | Chat completion and streaming boundary |
| [ChunkConsumer.java](../../core-engine/src/main/java/com/apex/core/port/ChunkConsumer.java) | Callback interface for streamed chunks |
| [HttpTransportPort.java](../../core-engine/src/main/java/com/apex/core/port/HttpTransportPort.java) | Buffered and streaming HTTP boundary |
| [JsonCodecPort.java](../../core-engine/src/main/java/com/apex/core/port/JsonCodecPort.java) | JSON encode/decode boundary |
| [PlannerPort.java](../../core-engine/src/main/java/com/apex/core/port/PlannerPort.java) | Goal planning and replanning |
| [RateLimiterPort.java](../../core-engine/src/main/java/com/apex/core/port/RateLimiterPort.java) | Request throttling |
| [SecretsPort.java](../../core-engine/src/main/java/com/apex/core/port/SecretsPort.java) | Credential storage and retrieval |
| [StoragePort.java](../../core-engine/src/main/java/com/apex/core/port/StoragePort.java) | Key-value persistence |
| [TelemetryPort.java](../../core-engine/src/main/java/com/apex/core/port/TelemetryPort.java) | Spans, counters, histograms, logs |
| [ToolExecutorPort.java](../../core-engine/src/main/java/com/apex/core/port/ToolExecutorPort.java) | Tool dispatch boundary |
| [VectorStorePort.java](../../core-engine/src/main/java/com/apex/core/port/VectorStorePort.java) | Vector storage and nearest-neighbour search |
| [RetryPolicy.java](../../core-engine/src/main/java/com/apex/core/port/RetryPolicy.java) | Exponential backoff with jitter, retryable status handling |
| [NoOpTelemetry.java](../../core-engine/src/main/java/com/apex/core/port/NoOpTelemetry.java) | Null-object telemetry implementation |
| [SafeTelemetryDecorator.java](../../core-engine/src/main/java/com/apex/core/port/SafeTelemetryDecorator.java) | Fault-isolating, redacting telemetry wrapper |

## Port models — `src/main/java/com/apex/core/port/model/`

| File | Purpose |
| :--- | :--- |
| [ApprovalDecision.java](../../core-engine/src/main/java/com/apex/core/port/model/ApprovalDecision.java) | Approval outcome plus decision metadata |
| [ApprovalRequest.java](../../core-engine/src/main/java/com/apex/core/port/model/ApprovalRequest.java) | Approval request passed to the gate |
| [AuditRecord.java](../../core-engine/src/main/java/com/apex/core/port/model/AuditRecord.java) | Audit record shape |
| [ChatDelta.java](../../core-engine/src/main/java/com/apex/core/port/model/ChatDelta.java) | Streamed delta token |
| [ChatRequest.java](../../core-engine/src/main/java/com/apex/core/port/model/ChatRequest.java) | Chat request payload |
| [ChatResponse.java](../../core-engine/src/main/java/com/apex/core/port/model/ChatResponse.java) | Chat response payload |
| [ExecutionResult.java](../../core-engine/src/main/java/com/apex/core/port/model/ExecutionResult.java) | Tool execution result (`ok`/`fail` factories) |
| [HttpChunk.java](../../core-engine/src/main/java/com/apex/core/port/model/HttpChunk.java) | Streamed HTTP chunk (data/done/error) |
| [HttpRequest.java](../../core-engine/src/main/java/com/apex/core/port/model/HttpRequest.java) | HTTP request value object |
| [HttpResponse.java](../../core-engine/src/main/java/com/apex/core/port/model/HttpResponse.java) | HTTP response value object |
| [Plan.java](../../core-engine/src/main/java/com/apex/core/port/model/Plan.java) | Multi-step plan |
| [PlanStep.java](../../core-engine/src/main/java/com/apex/core/port/model/PlanStep.java) | Single plan step |
| [ToolCall.java](../../core-engine/src/main/java/com/apex/core/port/model/ToolCall.java) | Model-requested tool invocation |
| [ToolSchema.java](../../core-engine/src/main/java/com/apex/core/port/model/ToolSchema.java) | Tool JSON schema description |

## Engine internals

| File | Purpose |
| :--- | :--- |
| [client/OpenAiCompatibleClient.java](../../core-engine/src/main/java/com/apex/core/client/OpenAiCompatibleClient.java) | Chat and streaming client for any OpenAI-compatible endpoint, built on `HttpTransportPort` |
| [json/MiniJson.java](../../core-engine/src/main/java/com/apex/core/json/MiniJson.java) | Dependency-free JSON reader/writer used inside the engine |
| [provider/ProviderConfig.java](../../core-engine/src/main/java/com/apex/core/provider/ProviderConfig.java) | Provider connection configuration |
| [provider/ProviderConfigurationManager.java](../../core-engine/src/main/java/com/apex/core/provider/ProviderConfigurationManager.java) | Provider configuration lifecycle in the engine |
| [provider/ProviderEngineContext.java](../../core-engine/src/main/java/com/apex/core/provider/ProviderEngineContext.java) | Wires engine ports for one provider |
| [provider/ProviderRegistry.java](../../core-engine/src/main/java/com/apex/core/provider/ProviderRegistry.java) | Registry of available providers |
| [tool/ToolRegistry.java](../../core-engine/src/main/java/com/apex/core/tool/ToolRegistry.java) | Tool name to `ToolWorker` registry |
| [tool/ToolWorker.java](../../core-engine/src/main/java/com/apex/core/tool/ToolWorker.java) | Tool contract: `getName`, `getDescription`, `execute(Map<String,String>)` |

## Tests — `src/test/java/com/apex/core/`

| File | Covers |
| :--- | :--- |
| [json/MiniJsonTest.java](../../core-engine/src/test/java/com/apex/core/json/MiniJsonTest.java) | JSON parsing and serialisation edge cases |
| [port/ApprovalPortContractTest.java](../../core-engine/src/test/java/com/apex/core/port/ApprovalPortContractTest.java) | `ApprovalPort` contract behaviour |
| [port/RetryPolicyTest.java](../../core-engine/src/test/java/com/apex/core/port/RetryPolicyTest.java) | Backoff sequence and retry predicates |
| [port/SafeTelemetryDecoratorTest.java](../../core-engine/src/test/java/com/apex/core/port/SafeTelemetryDecoratorTest.java) | Fault isolation and redaction |
| [port/model/ChatModelPortModelTest.java](../../core-engine/src/test/java/com/apex/core/port/model/ChatModelPortModelTest.java) | Port model invariants |
| [provider/ProviderConfigurationManagerTest.java](../../core-engine/src/test/java/com/apex/core/provider/ProviderConfigurationManagerTest.java) | Provider configuration behaviour |

24 test methods across these 6 classes. See [../testing.md](../testing.md).
