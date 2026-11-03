# `:app` — Android Jetpack Compose GUI Module

The `:app` module provides a modern, responsive Android interface built entirely in Jetpack Compose with Material 3. It manages provider persistence, hardware-backed credential storage, dynamic runtime model injection, and human-in-the-loop approval dialogs.

---

## Key Components

### 1. Dual Persistence Architecture

Sensitive and non-sensitive data are cleanly bifurcated:

```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     Non-Sensitive Settings      │       │       Sensitive API Keys        │
│   (ID, Name, Base URL, Model)   │       │                                 │
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
  - Persists provider configurations using Jetpack DataStore.
  - Implements connection diagnostics: executes `GET {baseUrl}/models` with fallback ping to `POST /chat/completions`.
- **`ProviderPresets`**:
  - Pre-configured defaults for Gemini (OpenAI compat), OpenRouter, Groq, DeepSeek, OpenAI, NVIDIA NIM, Ollama, and LM Studio.

---

## Dynamic Runtime Injection

- **`ProviderRegistry`**:
  - Collects `ProviderSettings` from DataStore into hot Kotlin `StateFlow` streams.
  - Enables instant switching of active AI backends across the entire application without recreating Activities or ViewModels.
- **`NarratorOrchestrator`**:
  - Consumes active provider settings injected by `ProviderRegistry`.
  - Dispatches turns to `UniversalAIGatewayImpl`.
  - Manages file creation permissions and surfaces self-improvement code modifications for user review.

---

## User Interface Screens

| Screen | Destination | Functionality |
| :--- | :--- | :--- |
| **Narrator Chat** | `chat` | Primary chat interface with live provider badge, quick model refresh dialog, and approval dialogs. |
| **Perchance Hub** | `perchance` | Native WebView wrapper for Perchance AI Chat Generator with external browser support (Brave/Firefox). |
| **Agent Swarm** | `agents` | Registry of specialized task-specific agent prompt cards. |
| **Workspace** | `workspace` | File explorer for reviewing and inspecting generated code. |
| **Settings** | `settings` | Provider management screen with preset pickers, key configuration, and live connection test. |
