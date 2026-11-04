# `:app` — Android Application

Jetpack Compose front end and the integration layer that wires the engine to the device.

| Setting | Value |
| :--- | :--- |
| Namespace | `com.example` |
| Application ID | `com.aistudio.apexagentic.masterchat` |
| Label | `Apex Agent` |
| Version | `versionCode 5`, `versionName 5.0` |
| SDK | `minSdk 24`, `compileSdk 36`, `targetSdk 36` |
| Languages | Kotlin 2.2.10, Compose (BOM from the version catalog), Java 11 compatibility |
| Key dependencies | Compose, Navigation, Room, DataStore, WorkManager, OkHttp, Retrofit/Moshi, Gson, Coil, markdown-compose, Firebase (AI, Firestore, Auth, AppCheck), credentials, Kotlin coroutines |
| Signing | `release` reads `KEYSTORE_PATH` (fallback `my-upload-key.jks`); `debugConfig` uses `debug.keystore` |

54 Kotlin source files under `app/src/main/java/com/example/`.

## Structure

```
com/example
├── MainActivity.kt                 # ComponentActivity -> MyApplicationTheme -> AppNavigation()
├── data
│   ├── adapter                     # OkHttpTransport, OrgJsonCodec, OtelTelemetry, TokenBucketRateLimiter
│   ├── local                       # AppDatabase (+ ApexDao), OfflineQueueDao
│   ├── model                       # Room entities and PendingActionEntity
│   └── provider                    # AndroidKeyStoreManager, DataStoreStorageAdapter,
│                                   # ProviderConfigurationManager, ProviderPresets
├── domain
│   ├── engine                      # AgentOrchestrator, AndroidApprovalPort, DiffUtil,
│   │                               # FileGovernanceEngine, ProviderRegistry, WorkspaceExporter
│   ├── gateway                     # AIGateway + UniversalAIGatewayImpl, AIResponse, ToolCall
│   ├── repository                  # ChatRepository(+Impl), SettingsRepository(+Impl)
│   ├── sync                        # AgentTaskWorker, OfflineSyncManager
│   ├── tools                       # 10 ToolWorker implementations
│   └── usecase                     # ApproveToolUse, LoadSettings, SendMessage
└── ui
    ├── chat                        # ChatScreen, ChatViewModel, ChatUiState
    ├── conversations · agents · workspace
    ├── settings                    # SettingsScreen, SettingsViewModel, SettingsUiState, ProviderConfigScreen
    ├── ProviderConfigScreen.kt     # unreferenced duplicate
    ├── navigation                  # AppNavigation, Screen routes
    └── theme                       # Color, Theme, Type
```

## Navigation

`AppNavigation.kt` declares five destinations: `chat/{chatId}`, `conversations`, `agents`,
`workspace`, `settings`.

## Tool workers

`read_file`, `read_file_range`, `write_file`, `run_command`, `http_get`, `http_post`, `create_tool`,
`delegate_task`, `save_memory`, `search_memory`. Each runs behind `SecurityInterlock`;
risk levels and review notes are in [../docs/security/tool-adapters.md](../docs/security/tool-adapters.md).

`RunCommandToolWorker` currently returns a formatted echo string and executes no process.

## Provider configuration

Seven presets ship in `ProviderPresets.kt` (`gemini_compat`, `openrouter`, `groq`, `deepseek`,
`openai`, `nvidia_nim`, `ollama_local`); any other OpenAI-compatible endpoint can be entered
manually. API keys are encrypted with an Android Keystore AES-256/GCM key before being stored in
DataStore, and `ProviderConfigurationManager.testConnection` verifies the endpoint.

## Persistence

| Store | Contents |
| :--- | :--- |
| Room (`AppDatabase`, schema version 9) | Agents, chat messages, conversations, workspace files and versions, memory items, lorebooks, provider configs, pending actions |
| DataStore | Provider preferences and key-value storage adapter state |
| Android Keystore | AES-256/GCM key protecting stored credentials |
| App files | Workspace sandbox at `filesDir/workspace`, limited to 10 MiB per file |

Room is configured with `fallbackToDestructiveMigration()`, so a schema change drops existing data.

## Tests

Unit: `ExampleUnitTest`, `ExampleRobolectricTest`, `e2e/AgentPipelineE2ETest`, `load/LoadSmokeTest`
(10 methods). Instrumented: `ExampleInstrumentedTest`.

```bash
gradle :app:testDebugUnitTest
```

## Known issues

Unresolved references in this module, the duplicate provider screen, and the unwired rate limiter are
listed with file and line numbers in [../docs/status.md](../docs/status.md). The module is not
expected to compile until those are addressed.
