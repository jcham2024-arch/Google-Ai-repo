# System Architecture & Code References

Apex Agentic Master Chat is built using strict Object-Oriented Programming principles and Clean Architecture in Kotlin & Jetpack Compose.

## Core Components
- **[[../app/src/main/java/com/example/domain/gateway/AIGateway.kt|AIGateway & GeminiGatewayImpl]]**: Multi-provider AI gateway supporting Google Gemini with Search Grounding (`gemini-3.8-flash`).
- **[[../app/src/main/java/com/example/domain/engine/FileGovernanceEngine.kt|FileGovernanceEngine]]**: Governs file creation (enabled by default) and self-editing code modification requiring mandatory user review and approval.
- **[[../app/src/main/java/com/example/ui/navigation/AppNavigation.kt|AppNavigation]]**: Manages navigation across Perchance Hub, Narrator Chat, Agent Registry, Workspace, and Settings.
