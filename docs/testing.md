# Testing Strategy

## Layers

| Layer | Framework | Location | Scope |
| :--- | :--- | :--- | :--- |
| Engine unit tests | JUnit 4 | `core-engine/src/test/java` | Ports, models, retry policy, telemetry decorator, provider configuration |
| Security unit tests | JUnit 4 | `security-interface/src/test/java` | Risk policy, interlock fail-closed paths, telemetry interceptor, audit chain |
| App unit tests | JUnit 4 + Robolectric | `app/src/test/java` | UI-adjacent logic, Robolectric resource access, pipeline integration, load smoke |
| App integration (E2E) | JUnit 4 + OkHttp MockWebServer | `app/src/test/java/com/example/e2e` | Full request/response pipeline against frozen LLM fixtures |
| Instrumented tests | AndroidX Test | `app/src/androidTest/java` | On-device execution, needs an emulator or device |

## Current inventory

| Module | Test classes | `@Test` methods | Files |
| :--- | ---: | ---: | :--- |
| `:core-engine` | 6 | 24 | `MiniJsonTest`, `ApprovalPortContractTest`, `RetryPolicyTest`, `SafeTelemetryDecoratorTest`, `ChatModelPortModelTest`, `ProviderConfigurationManagerTest` |
| `:security-interface` | 5 | 19 | `AuditLogManagerTest`, `FileAuditStoreTest`, `SecurityInterlockFailClosedTest`, `ToolInvocationInterceptorTest`, `SecurityInterlockTest` |
| `:app` (unit) | 4 | 10 | `ExampleUnitTest`, `ExampleRobolectricTest`, `e2e/AgentPipelineE2ETest`, `load/LoadSmokeTest` |
| `:app` (instrumented) | 1 | 1 | `ExampleInstrumentedTest` |

Totals: 53 unit `@Test` methods in 15 classes, plus 1 instrumented test. There is no coverage gate in
CI; the pipeline runs the suites and fails on test failure only.

## Running

```bash
gradle :core-engine:test              # fastest feedback
gradle :security-interface:test
gradle :app:testDebugUnitTest
gradle test                           # what CI runs
```

The repository has no Gradle wrapper, so `./gradlew` will not work until a wrapper is committed
(see [ci.md](ci.md) and [status.md](status.md)).

## What is well covered

- Audit chain construction, verification, tamper detection and rotation.
- Interlock fail-closed behaviour: null call, null approval port, mismatched approval request id,
  thrown delegate exception, budget exhaustion.
- Retry/backoff arithmetic and `Retry-After` handling.
- Telemetry fault isolation and redaction behaviour in `SafeTelemetryDecorator`.
- JSON parsing edge cases through `MiniJson`.

## What is not covered

- Compose UI behaviour: no `createComposeRule` tests exist; screens are untested.
- Room DAOs and migrations: no in-memory database tests.
- `AgentOrchestrator` tool-call loop, delegation and provider switching.
- WorkManager workers (`AgentTaskWorker`) and `OfflineSyncManager` replay.
- Firebase paths (AI, AppCheck, Firestore, Auth) require real credentials and are untested.
- `TokenBucketRateLimiter`, `FileGovernanceEngine`, `WorkspaceExporter`, `DiffUtil`, use cases.

## Known test defects

Verified by static inspection; see [status.md](status.md) for the method used.

| Location | Problem |
| :--- | :--- |
| `app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt:103,159,174` | References `OrgJsonCodec.INSTANCE`, but `OrgJsonCodec` has no companion object |
| `app/src/test/java/com/example/load/LoadSmokeTest.kt:73,139` | Calls `assertFalse(...)` without importing it |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt:19` | Asserts `app_name == "My Application"`, while `res/values/strings.xml` defines `Apex Agent` |

Until these are fixed the app unit-test task cannot compile, so a green CI run is not currently
achievable. The engine and security suites are independent of these defects.

## Adding tests

1. Engine or security logic: add a JUnit test next to the class under test in the module's
   `src/test/java` tree.
2. Network behaviour: extend the MockWebServer harness described in [e2e.md](e2e.md) and add a
   fixture under `app/src/test/resources/llm-fixtures/`.
3. Tool behaviour: exercise the worker directly and assert the interlock outcome (approval requested
   or denied) plus the audit entry count.
4. Keep new public engine types accompanied by a test; that is guardrail C4's intent even though CI
   does not check it.
