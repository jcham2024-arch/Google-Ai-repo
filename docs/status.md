# Implementation Status

Snapshot of what the repository actually contains today, with the evidence used. This file exists so
that the rest of the documentation does not have to overstate progress.

Method: static inspection of the working tree at the commit containing this document (file reads,
`grep`, and dependency inspection). No Gradle build or test run was executed while producing it,
because this environment has JDK 21 but no Gradle and no Android SDK installed.

---

## 1. Verified present

| Area | Evidence |
| :--- | :--- |
| Three-module Gradle build | `settings.gradle.kts` includes `:app`, `:core-engine`, `:security-interface` |
| Pure Java 11 engine with zero runtime dependencies | `core-engine/build.gradle.kts` declares only `testImplementation(libs.junit)` |
| 14 port interfaces + 3 helpers | 17 files in `core-engine/src/main/java/com/apex/core/port/` |
| 14 port models | 14 classes in `core-engine/src/main/java/com/apex/core/port/model/` |
| Risk classification and approval gate | `RiskLevel` L0–L4 with `requiresApproval()` at L3+, seeded map in `ToolRiskRegistry` |
| Fail-closed tool execution | `SecurityInterlock implements ToolExecutorPort` with null/exception/request-id checks |
| Tamper-evident audit chain | `AuditLogManager` + `FileAuditStore` (JSONL, SHA-256 chain, rotation, fsync) |
| Telemetry fault isolation | `SafeTelemetryDecorator` wraps every delegate call in `ToolInvocationInterceptor` |
| Provider configuration | 7 presets in `ProviderPresets.kt`; DataStore persistence + `testConnection` in `ProviderConfigurationManager` |
| Keystore-backed secrets | `AndroidKeyStoreManager` AES-256/GCM with Android Keystore |
| Compose UI with 5 destinations | `AppNavigation.kt` `chat/{chatId}`, `conversations`, `agents`, `workspace`, `settings` |
| 10 tool workers | `app/src/main/java/com/example/domain/tools/` |
| Unit tests | 53 `@Test` methods in 15 classes (core-engine 24, security-interface 19, app 10) |
| Instrumented test | `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` (1 test) |
| E2E fixtures | 5 files in `app/src/test/resources/llm-fixtures/` |
| CI pipeline | `.github/workflows/ci.yml` with guardrail steps C1, C2, C3, C4, C6, C8, C10 and gitleaks C5 |

## 2. Verified gaps in the application module

These are unresolved references found by static inspection: the named member or symbol has no
definition inside the module. Presume the app module does not compile until a build proves otherwise.

| Location | Problem |
| :--- | :--- |
| `app/src/main/java/com/example/domain/sync/AgentTaskWorker.kt:29,94` | Calls `ChatRepository.getInstance(...)`; `ChatRepository` (`domain/repository/ChatRepository.kt`) is an interface with no companion object and no such member anywhere in the module |
| `app/src/main/java/com/example/domain/sync/AgentTaskWorker.kt:52` | Calls `chatRepository.getAgentOrchestrator()`; not declared on `ChatRepository` or `ChatRepositoryImpl` |
| `app/src/main/java/com/example/domain/sync/AgentTaskWorker.kt:46` | Calls `OfflineSyncManager.getInstance(...)`; no `getInstance` definition exists in `domain/sync/OfflineSyncManager.kt` |
| `app/src/main/java/com/example/domain/repository/ChatRepositoryImpl.kt:37` | Calls `agentOrchestrator.executeToolCall(toolCall)`; `AgentOrchestrator.executeToolCall` is declared as `(String, String, Map<String,String>)` |
| `app/src/main/java/com/example/ui/chat/ChatScreen.kt:148` | Uses `Icons.Default.PlayArrow`, which is not imported (the import list has `Pause` and `Stop`) |
| `app/src/main/java/com/example/ui/chat/ChatScreen.kt:265` | `MaterialTheme.colorScheme.warning` is not part of the Material 3 color scheme |
| `app/src/main/java/com/example/ui/chat/ChatScreen.kt:610,612,617` | `colorScheme.onSurfaceContainerHighest` / `onSurfaceContainer` do not exist in Material 3 |
| `app/src/main/java/com/example/ui/chat/ChatScreen.kt:354` | `androidx.compose.ui.graphics.ImageBitmap.imageResource(bitmap)` is not a Compose API |
| `app/src/main/java/com/example/ui/navigation/AppNavigation.kt:23` | Imports `androidx.navigation.compose.navArgument`; the symbol lives in `androidx.navigation` |
| `app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt:103,159,174` | References `OrgJsonCodec.INSTANCE`; `OrgJsonCodec` is a plain class without a companion |
| `app/src/test/java/com/example/load/LoadSmokeTest.kt:73,139` | Calls `assertFalse(...)` without importing it |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt:19` | Asserts `app_name == "My Application"`, while `res/values/strings.xml` defines `"Apex Agent"` |

## 2b. Verified gaps in the security module

| Location | Problem |
| :--- | :--- |
| `security-interface/src/main/java/com/apex/security/interlock/SecurityInterlock.java:91,103,109` | Passes a `String` message where `AuditLogManager.createAndAppend(long, String, String, String, Map<String, String>)` expects an argument map; the only overload that exists takes a `Map`. These three paths do not compile. |

## 2c. Verified wiring gaps that change documented behaviour

| Area | Gap |
| :--- | :--- |
| Audit persistence | `ChatScreen.kt:819` builds `AuditLogManager()` with no `AuditStorePort`, so production keeps the chain in memory and writes no JSONL file. `FileAuditStore` is referenced only by tests (`AgentPipelineE2ETest`, `FileAuditStoreTest`). |
| Rate limiting | `TokenBucketRateLimiter` is constructed only in `LoadSmokeTest`, never by `OkHttpTransport`, so provider calls are unthrottled and its counters never fire in the app. |
| Operational states | `SystemStateManager` is never constructed and `transitionTo` is never called outside its own class, so `NORMAL`/`DEGRADED`/`SAFE_MODE` transitions do not happen automatically. |
| Read-tool path safety | `ReadFileToolWorker` and `ReadFileRangeToolWorker` resolve `File(baseDir, path)` without calling `FileGovernanceEngine.validatePath`, so traversal safety depends on the caller-supplied `baseDir`. |
| Room durability | `AppDatabase` uses `fallbackToDestructiveMigration()` with schema version 9 and `exportSchema = false`, so any schema change wipes user data and no schema history is emitted. |
| Release workflow | `.github/workflows/release.yml` calls `./gradlew` (no wrapper exists) and has no bare `gradle` fallback, so it fails at the build step; it also builds debug APKs and installs cosign without invoking it. |

## 3. Verified gaps in tooling and process

| Item | Current state |
| :--- | :--- |
| Gradle wrapper | No `gradlew` / `gradle/wrapper/gradle-wrapper.jar` in the repository, yet `ci.yml`, `release.yml` and `CONTRIBUTING.md` invoke `./gradlew` (CI also calls bare `gradle`). Drop the `./gradlew` invocations or commit a wrapper. |
| CI guardrail comment | `.github/workflows/ci.yml` implements C1, C2, C3, C4, C6, C8, C10 and runs gitleaks as C5. There is no C7, C9, C11–C13 step. The stale "C1 - C8" header comment was corrected in this documentation change; the numbering gap itself remains. |
| Guardrail numbering | Numbering now agrees between `ci.yml`, `docs/ci.md` and `CONTRIBUTING.md`. C7 and C9 stay conventions implemented in code (`ToolInvocationInterceptor`, `SafeTelemetryDecorator`) with no CI step, and no step enforces test coverage for new public types. |
| `run_command` tool | `RunCommandToolWorker` returns a formatted echo string; no process is executed. Documented behaviour is aspirational. |
| Rate limiting | `TokenBucketRateLimiter` implements `RateLimiterPort` but is used only by a test. |
| Duplicate screen | `ui/ProviderConfigScreen.kt` and `ui/settings/ProviderConfigScreen.kt` both declare a composable `ProviderConfigScreen`; only the `ui.settings` copy is reachable from navigation. |
| Composite build hygiene | The `:app` module ships Firebase (AI, Firestore, Auth, AppCheck), Retrofit/Moshi, Coil and markdown-compose. Documentation now states this; keep it in mind when reasoning about APK size, cold start and Play/Firebase requirements. |
| Audit privacy | `FileAuditStore`/`AuditLogManager` store argument hashes, not raw arguments, but there is no argument-redaction policy for values such as tokens inside tool arguments beyond the hash. |

## 4. Not verified here

- Compilation and test results for any module (no Gradle/SDK in this environment). The gaps in
  sections 2 and 2b predict compile failures; treat them as predictions until a build runs.
- Whether the remaining app source outside the listed lines compiles; only the symbols named above
  were checked.
- Runtime behaviour of Firebase AI, AppCheck, Firestore and Auth flows; these require device
  credentials and network access.
- Streaming performance, rate-limit behaviour under real providers, and audit rotation under load.

## 5. Documentation state

The documentation set was synchronised with the code in the same change that added this file:
counts, file index, guardrail list, module READMEs, runbooks and links were corrected. Where a
documented capability is not implemented, the document now says so explicitly instead of describing
the intended end state.
