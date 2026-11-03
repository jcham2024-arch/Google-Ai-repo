# System Architecture & Code References

Apex Agentic Master Chat is built using strict Object-Oriented Programming principles and Clean Architecture in Kotlin & Jetpack Compose.

## Core Components
- **[[../app/src/main/java/com/example/data/provider/ProviderConfigurationManager.kt|ProviderConfigurationManager]]**: Persists multiple AI provider settings (name, base URL, API key, default model, active status) using Android Jetpack `DataStore` (Preferences). Provides connection testing against `/models` or ping `/chat/completions`.
- **[[../app/src/main/java/com/example/data/provider/ProviderPresets.kt|ProviderPresets & ProviderSettings]]**: Pre-configured endpoint presets for OpenAI, OpenRouter, Groq, Gemini (OpenAI-compatible), NVIDIA NIM, Ollama, LM Studio, and Custom backends.
- **[[../app/src/main/java/com/example/domain/engine/ProviderRegistry.kt|ProviderRegistry]]**: Reactive in-memory registry holding all configured providers as StateFlows and dynamically injecting the active provider settings into the Narrator orchestrator.
- **[[../app/src/main/java/com/example/domain/engine/NarratorOrchestrator.kt|NarratorOrchestrator]]**: Master agent orchestrator that coordinates agentic turns, resolves injected provider settings dynamically, dispatches to `UniversalAIGatewayImpl`, and enforces file governance rules.
- **[[../app/src/main/java/com/example/domain/gateway/AIGateway.kt|UniversalAIGatewayImpl & AIGateway]]**: Unified Multi-Provider AI gateway supporting Google Gemini with Search Grounding (`gemini-3.8-flash`, `gemini-4-argon`, `gemini-3.1-flash-lite`) and OpenAI-compatible providers (OpenRouter, Groq Cloud, Local Ollama, and custom endpoints).
- **[[../app/src/main/java/com/example/domain/engine/FileGovernanceEngine.kt|FileGovernanceEngine]]**: Governs file creation (enabled by default) and self-editing code modification requiring mandatory user review and approval.
- **[[../app/src/main/java/com/example/ui/settings/SettingsScreen.kt|Provider Settings Hub]]**: UI for managing, editing, deleting, testing connections, and switching active AI providers backed by `DataStore`.
- **[[../app/src/main/java/com/example/ui/chat/ChatScreen.kt|Master Narrator Chat Screen]]**: Real-time chat interface connected to `NarratorOrchestrator`, displaying active injected provider/model and providing a fast refresh dialog.
- **[[../app/src/main/java/com/example/ui/navigation/AppNavigation.kt|AppNavigation]]**: Manages navigation across Perchance Hub, Narrator Chat, Agent Registry, Workspace, and Settings.
