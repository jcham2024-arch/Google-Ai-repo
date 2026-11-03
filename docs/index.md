# Apex Agentic Master Chat - Wiki Home

Welcome to the official wiki documentation for **Apex Agentic Master Chat**, an advanced offline-first AI chat and agent orchestration platform.

## Architecture & System Documentation
- **[[file_index|Master File Index & Repository Structure]]**: Full directory map and component registry across all three modules (`:app`, `:security-interface`, `:core-engine`).
- **[[architecture|System Architecture & Engines]]**: Technical specification of the three-tier multi-module architecture, ports and adapters, and security policies.
- **[[../README.md|Root System README]]**: Overview, setup instructions, and provider preset configuration.

## Core Modules
- **[[../core-engine/README.md|Core Engine Module]]**: Pure Java 11 headless engine with zero third-party dependencies and the Nine Port Interfaces.
- **[[../security-interface/README.md|Security Interface Module]]**: Risk level classification (`L0`–`L4`), human-in-the-loop approval gate, and SHA-256 tamper-evident audit logs.
- **[[../app/README.md|App GUI Module]]**: Jetpack Compose frontend, hardware-backed Android Keystore, and DataStore provider management.
