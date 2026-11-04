# `:core-engine` — Pure Java 11 Headless Engine

A `java-library` module with no runtime dependencies. The only declared dependency is
`testImplementation(libs.junit)`. The allow-list referenced by ADR 0003 and guardrail C1 lives in
[`allowed-deps.txt`](allowed-deps.txt).

## Principles

1. **Zero runtime dependencies.** `implementation(...)` must never appear in
   [`build.gradle.kts`](build.gradle.kts); CI fails the build if it does (guardrail C1).
2. **Java 11 bytecode.** `sourceCompatibility`/`targetCompatibility` are `VERSION_11`. Records,
   pattern matching and virtual threads are forbidden so the code stays compatible with Android ART on
   `minSdk 24`.
3. **Ports and adapters.** Orchestration depends only on interfaces in `com.apex.core.port`; Android
   and vendor specifics live in `:security-interface` and `:app`.

## Port interfaces (14)

| Port | Purpose | Used by |
| :--- | :--- | :--- |
| `ChatModelPort` | Chat completion and streaming | `OpenAiCompatibleClient` |
| `PlannerPort` | Goal planning and replanning | `:app` orchestration |
| `ToolExecutorPort` | Tool dispatch boundary | `SecurityInterlock`, `ToolInvocationInterceptor` |
| `ApprovalPort` | Human approval gate | `AndroidApprovalPort` |
| `StoragePort` | Key-value persistence | `DataStoreStorageAdapter` |
| `VectorStorePort` | Vector storage and search | `:app` memory features |
| `SecretsPort` | Credential storage | `AndroidKeyStoreManager` |
| `TelemetryPort` | Spans, counters, histograms, logs | `OtelTelemetry`, `NoOpTelemetry`, `SafeTelemetryDecorator` |
| `AuditPort` | Audit record sink | `AuditLogManager` |
| `AuditStorePort` | Audit read/verify surface | `FileAuditStore` |
| `HttpTransportPort` | Buffered and streaming HTTP | `OkHttpTransport` |
| `JsonCodecPort` | JSON encode/decode | `OrgJsonCodec` |
| `RateLimiterPort` | Request throttling | `TokenBucketRateLimiter` |
| `ChunkConsumer` | Streaming chunk callback | `OkHttpTransport.stream(...)` |

Helper types in the same package: `RetryPolicy` (attempts, exponential delay with jitter),
`NoOpTelemetry`, `SafeTelemetryDecorator` (fault isolation plus key-based redaction).

## Port models (14)

`ApprovalDecision`, `ApprovalRequest`, `AuditRecord`, `ChatDelta`, `ChatRequest`, `ChatResponse`,
`ExecutionResult`, `HttpChunk`, `HttpRequest`, `HttpResponse`, `Plan`, `PlanStep`, `ToolCall`,
`ToolSchema` — all immutable classes with null-safe constructors.

## Other packages

| Package | Contents |
| :--- | :--- |
| `client` | `OpenAiCompatibleClient` — chat and streaming against any OpenAI-compatible endpoint, built strictly on `HttpTransportPort` and `JsonCodecPort` |
| `json` | `MiniJson` — dependency-free JSON reader/writer; guardrail C4 keeps it inside this module |
| `provider` | `ProviderConfig`, `ProviderConfigurationManager`, `ProviderEngineContext`, `ProviderRegistry` |
| `tool` | `ToolRegistry` and the `ToolWorker` contract (`getName`, `getDescription`, `execute(Map<String,String>)`) implemented by `:app` |

## Tests

Six classes, 24 test methods: `MiniJsonTest`, `ApprovalPortContractTest`, `RetryPolicyTest`,
`SafeTelemetryDecoratorTest`, `ChatModelPortModelTest`, `ProviderConfigurationManagerTest`.

```bash
gradle :core-engine:test
```

## Local invariant checks

```bash
grep -E '^\s*implementation\(' core-engine/build.gradle.kts   # expect no output
grep -rn "import android\." core-engine/src/main/              # expect no output
grep -rn "record " core-engine/src/                            # Java 11 rule
```
