# `:app` — Android Jetpack Compose GUI Module

The `:app` module provides a modern, responsive Android interface built entirely in Jetpack Compose with Material 3. It manages provider persistence, hardware-backed credential storage, dynamic runtime model injection, editable model IDs, max token limit controls, and human-in-the-loop approval dialogs.

---

## Key Components

### 1. Dual Persistence Architecture

Sensitive and non-sensitive data are cleanly bifurcated:

```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     Non-Sensitive Settings      │       │       Sensitive API Keys        │
│(ID, Name, Base URL, Model, Max) │       │                                 │
├─────────────────────────────────┤       ├─────────────────────────────────┤
│ Jetpack DataStore (Preferences) │       │ Android Keystore (AES-256 GCM)  │
│  ProviderConfigurationManager   │       │      AndroidKeyStoreManager     │
└─────────────────────────────────┘       └─────────────────────────────────┘
```

- **`AndroidKeyStoreManager`**:
  - Generates an AES-256 key inside the device's hardware-backed Secure Element / TEE using Android Keystore.
  - Encrypts API keys before they are stored.
  - Keys are decrypted only in-memory immediately prior to dispatching network requests.
- **`ProviderConfigurationManager`**:
  - Persists provider configurations using Jetpack DataStore, including editable Model IDs (`updateModelId`) and max output tokens.
  - Implements connection diagnostics: executes `GET {baseUrl}/models` with fallback ping to `POST /chat/completions`.
- **`ProviderPresets`**:
  - Pre-configured defaults for Gemini (OpenAI compat), OpenRouter, Groq, DeepSeek, OpenAI, NVIDIA NIM, and Ollama.

---

## Dynamic Runtime Injection & Agent Orchestration

- **`ProviderRegistry`**:
  - Collects `ProviderSettings` from DataStore into hot Kotlin `StateFlow` streams.
  - Enables instant switching of active AI backends across the entire application without recreating Activities or ViewModels.
- **`AgentOrchestrator`**:
  - Consumes active provider settings injected by `ProviderRegistry`.
  - Dispatches task turns to `UniversalAIGatewayImpl` with model ID and `max_tokens` payload injection.
  - Manages tool execution through `ToolRegistry` and `SecurityInterlock` with step-by-step trace generation.

---

## User Interface Screens

| Screen | Destination | Functionality |
| :--- | :--- | :--- |
| **Agent Chat** | `chat` | Primary agent chat interface with live provider badge, execution trace, and approval dialogs. |
| **Agent Swarm** | `agents` | Registry of specialized task-specific sub-agent cards and system prompt overrides. |
| **Workspace** | `workspace` | Workspace explorer for inspecting workspace files and generated code. |
| **Settings** | `settings` | Provider management screen with preset pickers, editable model IDs, key configuration, and live connection test. |
