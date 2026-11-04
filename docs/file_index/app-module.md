# File Index — `:app`

Part of the [master file index](../file_index.md). 88 tracked files: 4 module files, 54 Kotlin
sources, 20 resource and manifest files, 4 unit-test files, 1 instrumented test, 5 fixtures.

Module root: [`app/build.gradle.kts`](../../app/build.gradle.kts) (`com.android.application`,
namespace `com.example`, application ID `com.aistudio.apexagentic.masterchat`, `versionCode 5`,
`versionName 5.0`, `compileSdk`/`targetSdk` 36, `minSdk 24`, Compose and BuildConfig enabled,
release signing reads `KEYSTORE_PATH` and falls back to `my-upload-key.jks`, debug signing uses
`debug.keystore`), [`app/README.md`](../../app/README.md),
[`app/proguard-rules.pro`](../../app/proguard-rules.pro), [`app/.gitignore`](../../app/.gitignore).

## Entry point and manifest

| File | Purpose |
| :--- | :--- |
| [MainActivity.kt](../../app/src/main/java/com/example/MainActivity.kt) | `ComponentActivity`; calls `enableEdgeToEdge()` and hosts `AppNavigation()` inside `MyApplicationTheme` |
| [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml) | `INTERNET` and `ACCESS_NETWORK_STATE` permissions, backup rules, single exported launcher activity |

## Data layer — `app/src/main/java/com/example/data/`

| File | Purpose |
| :--- | :--- |
| [adapter/OkHttpTransport.kt](../../app/src/main/java/com/example/data/adapter/OkHttpTransport.kt) | `HttpTransportPort` over OkHttp: retry loop with `RetryPolicy`, `Retry-After` handling, SSE `stream(...)` parsing |
| [adapter/OrgJsonCodec.kt](../../app/src/main/java/com/example/data/adapter/OrgJsonCodec.kt) | `JsonCodecPort` implemented with `org.json` |
| [adapter/OtelTelemetry.kt](../../app/src/main/java/com/example/data/adapter/OtelTelemetry.kt) | `TelemetryPort` backed by `android.util.Log`; inner `OtelSpan` implementation |
| [adapter/TokenBucketRateLimiter.kt](../../app/src/main/java/com/example/data/adapter/TokenBucketRateLimiter.kt) | `RateLimiterPort` token bucket (default capacity 60, refill 10/s); referenced nowhere else in the module |
| [local/AppDatabase.kt](../../app/src/main/java/com/example/data/local/AppDatabase.kt) | Room database and `ApexDao` (messages, conversations, agents, workspace versions, memory, lorebooks, provider configs) |
| [local/OfflineQueueDao.kt](../../app/src/main/java/com/example/data/local/OfflineQueueDao.kt) | Room DAO for the `pending_actions` offline queue, including pending-count flow |
| [model/Models.kt](../../app/src/main/java/com/example/data/model/Models.kt) | Room entities: `AgentCard`, `ChatMessage`, `WorkspaceFile`, `WorkspaceFileVersion`, `Lorebook`, `MemoryItem`, `ProviderConfig`, `ChatConversation` |
| [model/PendingActionEntity.kt](../../app/src/main/java/com/example/data/model/PendingActionEntity.kt) | `pending_actions` entity for queued offline work |
| [provider/AndroidKeyStoreManager.kt](../../app/src/main/java/com/example/data/provider/AndroidKeyStoreManager.kt) | Android Keystore AES-256/GCM encryption for API keys |
| [provider/DataStoreStorageAdapter.kt](../../app/src/main/java/com/example/data/provider/DataStoreStorageAdapter.kt) | `StoragePort` implemented over DataStore preferences |
| [provider/ProviderConfigurationManager.kt](../../app/src/main/java/com/example/data/provider/ProviderConfigurationManager.kt) | Provider persistence, active-provider selection, API key update and `testConnection` |
| [provider/ProviderPresets.kt](../../app/src/main/java/com/example/data/provider/ProviderPresets.kt) | `ProviderSettings` data class plus the 7 built-in presets |

## Domain layer — `app/src/main/java/com/example/domain/`

| File | Purpose |
| :--- | :--- |
| [engine/AgentOrchestrator.kt](../../app/src/main/java/com/example/domain/engine/AgentOrchestrator.kt) | Turn orchestration: builds function schemas from `ToolRegistry`, calls the gateway, executes tool calls, provider switching, active agent card, `orchestrateTurnWithImage` |
| [engine/AndroidApprovalPort.kt](../../app/src/main/java/com/example/domain/engine/AndroidApprovalPort.kt) | `ApprovalPort` implementation bridging to the Compose approval UI |
| [engine/DiffUtil.kt](../../app/src/main/java/com/example/domain/engine/DiffUtil.kt) | Line-level diff helper with `DiffLine`/`DiffType` |
| [engine/FileGovernanceEngine.kt](../../app/src/main/java/com/example/domain/engine/FileGovernanceEngine.kt) | Sandboxed workspace file governance: `WorkspaceEvent` stream, size limits, version rows through `ApexDao`, `SandboxStats` |
| [engine/ProviderRegistry.kt](../../app/src/main/java/com/example/domain/engine/ProviderRegistry.kt) | Reactive provider registry exposing `activeProvider` and provider CRUD plus connection test |
| [engine/WorkspaceExporter.kt](../../app/src/main/java/com/example/domain/engine/WorkspaceExporter.kt) | Zips the workspace directory into external storage through MediaStore |
| [gateway/AIGateway.kt](../../app/src/main/java/com/example/domain/gateway/AIGateway.kt) | `AIGateway` interface (`generateChatResponse`, `getEmbeddings`), `AIResponse`/`ToolCall` models, and `UniversalAIGatewayImpl` with Gemini and OpenAI-compatible paths |
| [repository/ChatRepository.kt](../../app/src/main/java/com/example/domain/repository/ChatRepository.kt) | Interface: `getMessagesForChat`, `saveMessage`, `executeAgentTurn`, `executeTool` |
| [repository/ChatRepositoryImpl.kt](../../app/src/main/java/com/example/domain/repository/ChatRepositoryImpl.kt) | Room-backed implementation delegating turns to `AgentOrchestrator` |
| [repository/SettingsRepository.kt](../../app/src/main/java/com/example/domain/repository/SettingsRepository.kt) | Interface for provider read/save/delete, active provider and connection test |
| [repository/SettingsRepositoryImpl.kt](../../app/src/main/java/com/example/domain/repository/SettingsRepositoryImpl.kt) | `SettingsRepository` over `ProviderConfigurationManager` |
| [sync/AgentTaskWorker.kt](../../app/src/main/java/com/example/domain/sync/AgentTaskWorker.kt) | WorkManager `CoroutineWorker` running an agent turn (with optional image) in the background, plus `createWorkRequest` |
| [sync/OfflineSyncManager.kt](../../app/src/main/java/com/example/domain/sync/OfflineSyncManager.kt) | Offline action queueing and replay over `OfflineQueueDao`, exposes pending count |
| [tools/CreateToolToolWorker.kt](../../app/src/main/java/com/example/domain/tools/CreateToolToolWorker.kt) | Tool `create_tool`: registers a new tool definition at runtime |
| [tools/DelegateTaskToolWorker.kt](../../app/src/main/java/com/example/domain/tools/DelegateTaskToolWorker.kt) | Tool `delegate_task`: hands a sub-task to another agent through the interlock |
| [tools/HttpGetToolWorker.kt](../../app/src/main/java/com/example/domain/tools/HttpGetToolWorker.kt) | Tool `http_get` |
| [tools/HttpPostToolWorker.kt](../../app/src/main/java/com/example/domain/tools/HttpPostToolWorker.kt) | Tool `http_post` |
| [tools/ReadFileToolWorker.kt](../../app/src/main/java/com/example/domain/tools/ReadFileToolWorker.kt) | Tool `read_file`, sandbox-relative |
| [tools/ReadFileRangeToolWorker.kt](../../app/src/main/java/com/example/domain/tools/ReadFileRangeToolWorker.kt) | Tool `read_file_range` for partial reads |
| [tools/RunCommandToolWorker.kt](../../app/src/main/java/com/example/domain/tools/RunCommandToolWorker.kt) | Tool `run_command`; returns a formatted echo string, executes no process |
| [tools/SaveMemoryToolWorker.kt](../../app/src/main/java/com/example/domain/tools/SaveMemoryToolWorker.kt) | Tool `save_memory` |
| [tools/SearchMemoryToolWorker.kt](../../app/src/main/java/com/example/domain/tools/SearchMemoryToolWorker.kt) | Tool `search_memory` |
| [tools/WriteFileToolWorker.kt](../../app/src/main/java/com/example/domain/tools/WriteFileToolWorker.kt) | Tool `write_file` |
| [usecase/ApproveToolUseCase.kt](../../app/src/main/java/com/example/domain/usecase/ApproveToolUseCase.kt) | Applies an approval decision to a pending tool call |
| [usecase/LoadSettingsUseCase.kt](../../app/src/main/java/com/example/domain/usecase/LoadSettingsUseCase.kt) | Reads providers and active provider, saves provider settings |
| [usecase/SendMessageUseCase.kt](../../app/src/main/java/com/example/domain/usecase/SendMessageUseCase.kt) | Sends a message, optionally deferring to `OfflineSyncManager`; serialises transit with Gson |

## UI layer — `app/src/main/java/com/example/ui/`

| File | Purpose |
| :--- | :--- |
| [navigation/AppNavigation.kt](../../app/src/main/java/com/example/ui/navigation/AppNavigation.kt) | `Screen` sealed class with routes `chat/{chatId}`, `conversations`, `agents`, `workspace`, `settings`, bottom navigation, `NavHost` |
| [chat/ChatScreen.kt](../../app/src/main/java/com/example/ui/chat/ChatScreen.kt) | Main chat surface: message list, tool status, approval sheet, constructs `ToolRegistry`/`SecurityInterlock`/`AuditLogManager`/`AndroidApprovalPort` |
| [chat/ChatViewModel.kt](../../app/src/main/java/com/example/ui/chat/ChatViewModel.kt) | Chat state, WorkManager enqueue/cancel, provider and approval state |
| [chat/ChatUiState.kt](../../app/src/main/java/com/example/ui/chat/ChatUiState.kt) | UI state models: `AgentExecutionStep`, `DelegationInfo`, `ToolApprovalUiState` |
| [conversations/ConversationListScreen.kt](../../app/src/main/java/com/example/ui/conversations/ConversationListScreen.kt) | Conversation list and creation |
| [agents/AgentListScreen.kt](../../app/src/main/java/com/example/ui/agents/AgentListScreen.kt) | Agent swarm list |
| [workspace/WorkspaceScreen.kt](../../app/src/main/java/com/example/ui/workspace/WorkspaceScreen.kt) | Workspace browser with diff/version actions |
| [settings/SettingsScreen.kt](../../app/src/main/java/com/example/ui/settings/SettingsScreen.kt) | Settings surface linking to provider configuration |
| [settings/SettingsViewModel.kt](../../app/src/main/java/com/example/ui/settings/SettingsViewModel.kt) | Settings state and provider actions |
| [settings/SettingsUiState.kt](../../app/src/main/java/com/example/ui/settings/SettingsUiState.kt) | Settings UI state model |
| [settings/ProviderConfigScreen.kt](../../app/src/main/java/com/example/ui/settings/ProviderConfigScreen.kt) | Provider editor reachable from navigation: presets, base URL, key, model, connection test |
| [ProviderConfigScreen.kt](../../app/src/main/java/com/example/ui/ProviderConfigScreen.kt) | Second, unreferenced `ProviderConfigScreen` composable (duplicate of the settings copy) |
| [theme/Color.kt](../../app/src/main/java/com/example/ui/theme/Color.kt) | Color definitions |
| [theme/Theme.kt](../../app/src/main/java/com/example/ui/theme/Theme.kt) | `MyApplicationTheme` with dynamic/light/dark color schemes |
| [theme/Type.kt](../../app/src/main/java/com/example/ui/theme/Type.kt) | Typography scale |

## Resources

| Path | Purpose |
| :--- | :--- |
| `res/values/strings.xml` | `app_name` = `Apex Agent` |
| `res/values/colors.xml` | Purple/teal/black/white palette |
| `res/values/themes.xml` | `Theme.MyApplication` extending `android:Theme.DeviceDefault.NoActionBar` |
| `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Backup and device-transfer rules referenced by the manifest |
| `res/drawable/ic_launcher_background.xml`, `res/drawable/ic_launcher_foreground.xml` | Adaptive icon layers |
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Adaptive icon definitions |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher*.webp` | Density-specific launcher bitmaps (10 files) |

## Tests and fixtures

| File | Purpose |
| :--- | :--- |
| [test/java/com/example/ExampleUnitTest.kt](../../app/src/test/java/com/example/ExampleUnitTest.kt) | Local unit test smoke case |
| [test/java/com/example/ExampleRobolectricTest.kt](../../app/src/test/java/com/example/ExampleRobolectricTest.kt) | Robolectric test; currently asserts the old `app_name` value `My Application` |
| [test/java/com/example/e2e/AgentPipelineE2ETest.kt](../../app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt) | MockWebServer-driven pipeline tests using the fixtures below |
| [test/java/com/example/load/LoadSmokeTest.kt](../../app/src/test/java/com/example/load/LoadSmokeTest.kt) | Load smoke test over the pipeline |
| [androidTest/java/com/example/ExampleInstrumentedTest.kt](../../app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt) | Instrumented test case for on-device runs |
| [test/resources/llm-fixtures/simple-reply.json](../../app/src/test/resources/llm-fixtures/simple-reply.json) | Plain completion response |
| [test/resources/llm-fixtures/tool-call-single.json](../../app/src/test/resources/llm-fixtures/tool-call-single.json) | One `read_file` tool call |
| [test/resources/llm-fixtures/tool-call-nested-args.json](../../app/src/test/resources/llm-fixtures/tool-call-nested-args.json) | Tool call with nested argument objects |
| [test/resources/llm-fixtures/streamed-reply.sse](../../app/src/test/resources/llm-fixtures/streamed-reply.sse) | SSE stream with incremental deltas and `[DONE]` |
| [test/resources/llm-fixtures/rate-limited-then-ok.json](../../app/src/test/resources/llm-fixtures/rate-limited-then-ok.json) | 429 followed by a successful retry |

Known unresolved references in this module are listed in [../status.md](../status.md).
