# Apex Agentic Master Chat

Offline-first multi-agent orchestration app built as a three-module Gradle project: a pure Java 11
core engine, an Android security interlock library, and a Jetpack Compose application.

App identity: application ID `com.aistudio.apexagentic.masterchat`, label `Apex Agent`,
`versionCode 5`, `versionName 5.0`, `minSdk 24`, `compileSdk 36`, `targetSdk 36`.

```
┌──────────────────────────────────────────────────────────────┐
│                            :app                              │
│  Compose UI • Room • DataStore • WorkManager • OkHttp        │
│  Firebase (AI, Firestore, Auth, AppCheck) • Retrofit/Moshi   │
└───────────────────────────┬──────────────────────────────────┘
                            │ depends on
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                     :security-interface                      │
│   SecurityInterlock • Risk policies L0–L4 • SHA-256 audit    │
└───────────────────────────┬──────────────────────────────────┘
                            │ depends on
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                        :core-engine                          │
│  Pure Java 11 • zero runtime dependencies • ports + adapters │
└──────────────────────────────────────────────────────────────┘
```

Dependency direction is enforced in CI: no module may depend upward.

---

## Repository layout

```
apex-android/
├── README.md                     # this file
├── CONTRIBUTING.md               # setup, guardrails, PR checklist
├── CHANGELOG.md                  # release notes
├── SECURITY.md · CODE_OF_CONDUCT.md · CODEOWNERS
├── settings.gradle.kts           # includes :app, :core-engine, :security-interface
├── build.gradle.kts              # root build (plugin aliases only)
├── gradle/libs.versions.toml     # version catalog
├── gradle.properties
├── metadata.json                 # AI Studio platform metadata
├── .env.example                  # template consumed by the Secrets Gradle plugin
├── .github/                      # workflows (ci.yml, release.yml), issue/PR templates
├── .aistudio/artifacts/brain/    # imported AI Studio implementation plan
├── public/assets/aistudio/       # imported AI Studio markdown exports
├── docs/                         # architecture, operations and security docs
├── core-engine/                  # pure Java 11 engine (java-library)
├── security-interface/           # Android library (com.apex.security)
└── app/                          # Android application (com.example)
```

Full per-file index: **[docs/file_index.md](docs/file_index.md)**.

---

## Requirements

- JDK 17 (CI uses Temurin 17).
- Android SDK with build tools for `compileSdk 36`.
- Gradle: **this repository has no Gradle wrapper** (`gradlew` is absent). Use an installed
  `gradle` (CI runs `gradle test --no-daemon` and `gradle assembleDebug --no-daemon`) or open the
  project in Android Studio.
- Optional: a local `.env` for the Secrets Gradle plugin.

## Build and test

```bash
gradle :core-engine:test          # engine ports, retry, telemetry decorator
gradle :security-interface:test   # interlock, audit chain, risk policies
gradle :app:testDebugUnitTest     # Robolectric + MockWebServer suites
gradle test                       # everything (what CI runs)
gradle assembleDebug              # debug APK -> app/build/outputs/apk/debug/
```

Release builds read keystore material from the environment (`KEYSTORE_PATH`, `STORE_PASSWORD`,
`KEY_PASSWORD`); the debug variant uses the committed `debug.keystore` configuration.

---

## Core engine: ports and adapters

`:core-engine` exposes **14 port interfaces** plus 3 helper types in `com.apex.core.port`, and
14 immutable model classes in `com.apex.core.port.model`.

| Port | Purpose | App-side implementation |
| :--- | :--- | :--- |
| `ChatModelPort` | Chat completion and streaming | `OpenAiCompatibleClient` (core-engine) |
| `HttpTransportPort` | Buffered and streaming HTTP | `OkHttpTransport` |
| `ChunkConsumer` | Streaming chunk callback | inline lambdas in `OkHttpTransport` |
| `JsonCodecPort` | JSON encode/decode | `OrgJsonCodec` |
| `PlannerPort` | Goal planning and replanning | orchestrated in `AgentOrchestrator` |
| `ToolExecutorPort` | Tool dispatch boundary | `SecurityInterlock` (security-interface) |
| `ApprovalPort` | Human-in-the-loop gate | `AndroidApprovalPort` |
| `StoragePort` | Key-value persistence | `DataStoreStorageAdapter` |
| `VectorStorePort` | Vector storage / search | in-memory (`AgentOrchestrator` memory tooling) |
| `SecretsPort` | Credential storage | `AndroidKeyStoreManager` |
| `RateLimiterPort` | Request throttling | `TokenBucketRateLimiter` (defined, not yet wired) |
| `TelemetryPort` | Spans, counters, histograms, logs | `OtelTelemetry` (+ Logcat) |
| `AuditPort` | Audit record sink | `FileAuditStore` |
| `AuditStorePort` | Audit read/verify surface | `FileAuditStore` |

Helper types: `RetryPolicy` (exponential backoff with jitter), `NoOpTelemetry`,
`SafeTelemetryDecorator` (never throws into the call path).

## Security model

- `SecurityInterlock implements ToolExecutorPort`: every model-initiated tool call passes the risk
  registry, then the approval gate, then the audit sink. Fail-closed on null calls, thrown
  exceptions and mismatched approval request ids.
- Risk levels `L0`–`L4`; `L3` and `L4` require approval (`RiskLevel.requiresApproval()`).
  Seeded mapping: `read_file` L2, `list_files` L2, `write_file` L3, `write_file_external` L3,
  `http_get` L2, `http_post` L3, `run_command` L4, unknown tool L3.
- Audit chain: JSONL records, genesis hash of 64 zeros,
  `entryHash = SHA-256(timestamp:tool:risk:decision:argsHash:prevHash)`, verified by
  `AuditLogManager.verify()`, rotated and fsynced by `FileAuditStore`.
- API keys are encrypted with an Android Keystore AES-256/GCM key; telemetry log fields pass
  through `SafeTelemetryDecorator` redaction.

## Supported provider presets

Seven presets ship in `app/src/main/java/com/example/data/provider/ProviderPresets.kt`:

| Preset id | Base URL | Default model |
| :--- | :--- | :--- |
| `gemini_compat` | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash` |
| `openrouter` | `https://openrouter.ai/api/v1` | `meta-llama/llama-3.3-70b-instruct` |
| `groq` | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` |
| `deepseek` | `https://api.deepseek.com/v1` | `deepseek-chat` |
| `openai` | `https://api.openai.com/v1` | `gpt-4o` |
| `nvidia_nim` | `https://integrate.api.nvidia.com/v1` | `meta/llama-3.3-70b-instruct` |
| `ollama_local` | `http://10.0.2.2:11434/v1` | `llama3` |

Any other OpenAI-compatible endpoint can be entered manually in the provider screen.

## Application surface

Five navigation destinations (`app/src/main/java/com/example/ui/navigation/AppNavigation.kt`):
`chat/{chatId}`, `conversations`, `agents`, `workspace`, `settings`.

Ten tool workers register under `com.example.domain.tools`: `read_file`, `read_file_range`,
`write_file`, `run_command`, `http_get`, `http_post`, `create_tool`, `delegate_task`, `save_memory`,
`search_memory`.

## Documentation map

| Document | Contents |
| :--- | :--- |
| [docs/index.md](docs/index.md) | Wiki home and full documentation map |
| [docs/file_index.md](docs/file_index.md) | Repository file index (hub) |
| [docs/architecture.md](docs/architecture.md) | Modules, layering, ports, runtime flows |
| [docs/status.md](docs/status.md) | Verified implementation status and known gaps |
| [docs/testing.md](docs/testing.md) | Test strategy and current test inventory |
| [docs/e2e.md](docs/e2e.md) | End-to-end harness and fixtures |
| [docs/ci.md](docs/ci.md) | CI pipeline and guardrails C1–C10 |
| [docs/networking.md](docs/networking.md) | HTTP, streaming, retries, timeouts |
| [docs/observability.md](docs/observability.md) | Telemetry ports, signals, fault isolation |
| [docs/audit-format.md](docs/audit-format.md) | Tamper-evident audit record format |
| [docs/security/threat-model.md](docs/security/threat-model.md) | Trust boundaries and threats |
| [docs/runbooks/](docs/runbooks/) | Operational runbooks |

## Verification snippets

```bash
# core-engine must not gain runtime dependencies
grep -n "implementation(" core-engine/build.gradle.kts    # expects no match
# core-engine must stay free of Android and desktop-only APIs
grep -rn "import android\.\|Thread.ofVirtual\|java.net.http\|ProcessHandle" core-engine/src
# core-engine must stay Java 11 compatible (no records)
grep -rn "record " core-engine/src
```

Known gaps and unverified areas are listed in [docs/status.md](docs/status.md) rather than hidden
here.
