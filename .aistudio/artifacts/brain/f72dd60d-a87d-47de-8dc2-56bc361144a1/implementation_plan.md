# Implementation Plan - Apex Agentic Master Chat

Apex Agentic Master Chat is a production-grade, highly secure, standalone Android application in Kotlin built with Jetpack Compose and Material 3. It provides an advanced offline-first AI chat and agent orchestration platform with Perchance integration, web search grounding, self-improvement capabilities, and granular permission controls.

---

## Core Requirements & Architecture

1. **App Naming & Identity:**
   - App Name: **Apex Agentic Master Chat** (updated in `metadata.json`, `strings.xml`, and `settings.gradle.kts`).
2. **Perchance Generator WebView Wrapper:**
   - Native GUI tab hosting the Perchance AI Chat Generator to create specialized agent cards, lorebooks, and resources directly inside the app.
3. **Web Search Grounding:**
   - Integrated Google Search grounding via `gemini-3.8-flash` to provide up-to-date and accurate information during agent chat orchestration.
4. **Granular Permissions & Self-Improvement Governance:**
   - **File Creation:** Enabled by default.
   - **Self-Editing / Code Modification:** Disabled by default. Enabled per chat message/task as needed, requiring mandatory user review and explicit approval via an in-app diff/approval modal before any code edit is applied.
5. **Multi-Provider AI Gateway & MCP Tools:**
   - OpenAI-compatible multi-provider routing (Gemini default, OpenRouter, Groq, NVIDIA NIM, local) and Model Context Protocol (MCP) tool support.

---

## Phased Implementation Roadmap

### Phase 1: Foundation, App Renaming & Perchance WebView GUI
- Update `metadata.json`, `strings.xml`, and `settings.gradle.kts` to **Apex Agentic Master Chat**.
- Implement `PerchanceBrowserScreen` (WebView wrapper with full navigation controls, file uploads, and JS execution).
- Implement main bottom navigation / navigation rail framework.

### Phase 2: Web Search Grounding & Multi-Provider AI Gateway
- Implement `GeminiGatewayImpl` configured with Google Search grounding tool (`gemini-3.8-flash`).
- Set up EncryptedSharedPreferences for API keys and provider configurations.

### Phase 3: Agent Swarm, File Creation & Self-Editing Review Sandbox
- Build agent card registry and task orchestration.
- Implement `FileGovernanceEngine` with file creation support (default ON) and self-editing code modification sandbox requiring explicit per-message user review and approval before writing files.

### Phase 4: Persistence, Firebase Cloud Sync & Wiki Docs
- Room database for offline-first caching and Firebase Firestore + Google Sign-In for cloud persistence.
- Generate obsidian-style wiki documentation in `/docs/`.
