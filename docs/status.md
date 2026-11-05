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

A real `:app:compileDebugKotlin` run now exists. Result: **286 Kotlin errors across 18 files**, all
pre-dating the current fix series (none originate in `WorkspacePathGuard`, the read workers,
`RunCommandToolWorker`, the `FileAuditStore` wiring or the mikepenz `Markdown` block in
`ChatScreen`). Per-file counts: `ChatScreen.kt` 79, `FileGovernanceEngine.kt` 40,
`SearchMemoryToolWorker.kt` 24, `WorkspaceScreen.kt` 23, `AppNavigation.kt` 22,
`ChatViewModel.kt` 21, `AndroidApprovalPort.kt` 15, KSP-generated `ApexDao_Impl.kt` 12,
`AgentTaskWorker.kt` 9, `AIGateway.kt` 7, `WorkspaceExporter.kt` 7, `SaveMemoryToolWorker.kt` 6,
`DelegateTaskToolWorker.kt` 6, `ConversationListScreen.kt` 5, `ChatRepositoryImpl.kt` 4,
`AgentOrchestrator.kt` 3, `AppDatabase.kt` 2, `AgentListScreen.kt` 1. Dominant error classes:
`'return' is prohibited here` (25), type-inference failures (24), `@Composable` context violations
(18), `Unresolved reference 'it'` (8) and tokenizer-level syntax errors (8+5) that suggest
structurally broken regions in `ChatScreen`, `WorkspaceScreen` and `AppNavigation`.

The static list below was produced before the first build ran and is the evidence trail for the
root causes behind those counts:

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

No open entries. The three non-compiling `createAndAppend` call sites
(`SecurityInterlock.java:91,103,109`) now pass an argument map through a `denialArguments` helper that
keeps the denial reason inside the hashed audit domain, and the four stale string call sites in
`AuditLogManagerTest` were moved to the canonicalizing overload.

## 2c. Verified wiring gaps that change documented behaviour

| Area | Gap |
| :--- | :--- |
| Rate limiting | `TokenBucketRateLimiter` is constructed only in `LoadSmokeTest`, never by `OkHttpTransport`, so provider calls are unthrottled and its counters never fire in the app. |
| Operational states | `SystemStateManager` is never constructed and `transitionTo` is never called outside its own class, so `NORMAL`/`DEGRADED`/`SAFE_MODE` transitions do not happen automatically. |
| Room durability | `AppDatabase` uses `fallbackToDestructiveMigration()` with schema version 9 and `exportSchema = false`, so any schema change wipes user data and no schema history is emitted. |

## 3. Verified gaps in tooling and process

| Item | Current state |
| :--- | :--- |
| Gradle wrapper | `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are committed and match the 9.3.1 distribution pinned in `gradle-wrapper.properties`. Note that this workspace lives on shared storage, which is not exec-capable, so `./gradlew` fails here with `bad interpreter: Permission denied`; run `sh gradlew` on the phone. |
| CI guardrail comment | `.github/workflows/ci.yml` implements C1, C2, C3, C4, C6, C8, C10 and runs gitleaks as C5. There is no C7, C9, C11–C13 step. The stale "C1 - C8" header comment was corrected in this documentation change; the numbering gap itself remains. |
| Guardrail numbering | Numbering now agrees between `ci.yml`, `docs/ci.md` and `CONTRIBUTING.md`. C7 and C9 stay conventions implemented in code (`ToolInvocationInterceptor`, `SafeTelemetryDecorator`) with no CI step, and no step enforces test coverage for new public types. |
| `run_command` tool | `RunCommandToolWorker` fails closed with `UnsupportedOperationException` and executes nothing; it never reports a fabricated success. Still registered at L4 and still advertised in the model tool schema, so a turn can be spent on a call that always fails. |
| Read-path sandbox root | `ReadFileToolWorker` and `ReadFileRangeToolWorker` now validate through `WorkspacePathGuard` against the same `filesDir/workspace` root as `FileGovernanceEngine`. Files outside that root are no longer readable by the model, which is a behaviour change from the previous `filesDir`-wide read surface. |
| Rate limiting | `TokenBucketRateLimiter` implements `RateLimiterPort` but is used only by a test. |
| Duplicate screen | `ui/ProviderConfigScreen.kt` and `ui/settings/ProviderConfigScreen.kt` both declare a composable `ProviderConfigScreen`; only the `ui.settings` copy is reachable from navigation. |
| Markdown renderer | `com.github.gfx:markdown-compose` is gone from GitHub and unresolvable from JitPack; `gradle/libs.versions.toml` now pins `com.mikepenz:multiplatform-markdown-renderer-m3:0.40.0`, the newest release whose Kotlin metadata the project's Kotlin 2.2.10 compiler can read (0.43.0+ are built with Kotlin 2.4). The `ChatScreen` render block uses the mikepenz `-m3` API. |
| Composite build hygiene | The `:app` module ships Firebase (AI, Firestore, Auth, AppCheck), Retrofit/Moshi, Coil and markdown rendering. Documentation now states this; keep it in mind when reasoning about APK size, cold start and Play/Firebase requirements. |
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
