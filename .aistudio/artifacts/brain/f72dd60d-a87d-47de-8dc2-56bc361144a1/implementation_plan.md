# Document 03 — Long-Term Production Readiness & Provider Configuration UI

This implementation plan specifies the architecture and execution strategy for **Provider Configuration UI** and **Document 03 (Long-Term Production Readiness: Scale, Operations, Governance, and Sustainability)** for `ApexAgentManagementChat` / `apex-android`.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following technical choices were confirmed during Phase 1 clarification:

- **Confirmed Decision 1 (Provider Configuration UI)**: Enhance the `SettingsScreen.kt` Jetpack Compose UI with dedicated preset cards (DeepSeek, OpenRouter, Groq, Gemini, OpenAI, NVIDIA NIM, Ollama, LM Studio), inline Base URL and Model ID fields, live connection diagnostic testing, and an encryption dialog connected to `ProviderConfigurationManager` and hardware-backed `AndroidKeyStoreManager`.
- **Confirmed Decision 2 (Core Engine Dependency Policy - ADR 0003)**: Adopt a vetted allowlist policy recorded in `docs/adr/0003-core-engine-dependencies.md` and enforced via `core-engine/allowed-deps.txt` in CI, permitting zero-transitive-dependency libraries that pass security and scorecards.
- **Confirmed Decision 3 (Scalability, Rate Limiting & Degradation)**: Implement a token-bucket `RateLimiterPort` in `:core-engine` with queuing and automatic fallback to degraded mode (`Normal`, `Degraded`, `Safe Mode`) during provider outages or telemetry failures.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an intuitive Jetpack Compose UI for multi-provider AI endpoint configuration while establishing long-term production readiness: rate limiting, operational runbooks, release engineering (signed tagged artifacts), automated changelog enforcement, and contributor governance.
- **Target Audience**: End users configuring custom LLM provider credentials, as well as operations engineers and open-source contributors managing production deployments.
- **Key Value**: Delivers seamless provider key management on-device while transforming the repository into a fully governed, release-ready, multi-module Android engine.

---

## 2. User Experience & Visual Design

### Provider Configuration Screen (`SettingsScreen.kt`)
- **Preset Cards Grid**: Interactive Material 3 cards for DeepSeek, OpenRouter, Groq, Gemini, OpenAI, NVIDIA NIM, Ollama, and LM Studio.
- **Inline Endpoint Settings**: Editable Base URL and default Model ID fields per provider with instant input validation.
- **Hardware-Secured API Key Dialog**: Modal dialog for entering and updating provider API keys encrypted via AES-256 GCM in `AndroidKeyStoreManager`.
- **Live Connection Diagnostics**: "Test Connection" button executing real-time `HttpTransportPort` health checks with animated latency badge and status indicators (`Connected`, `Unauthorized`, `Unreachable`).
- **Active Model Selector**: Floating chip bar permitting instant Narrator model switching across registered providers.

---

## 3. Technical Architecture & System Layout

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                    :app                                       │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌─────────────────────┐  │
│  │ SettingsScreen (UI)    │ │ ProviderConfigManager│ │ AndroidKeyStore     │  │
│  │ Preset Cards & Dialogs │ │  (DataStore Sync)    │ │ (AES-256 Key Encryption│
│  └───────────┬────────────┘ └──────────┬───────────┘ └──────────┬──────────┘  │
└──────────────┼─────────────────────────┼────────────────────────┼─────────────┘
               │ binds to                │ manages                │ secures
               ▼                         ▼                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                             :security-interface                               │
│  ┌──────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────┐ │
│  │ ToolInvocationInterceptor│ │    SecurityInterlock    │ │ FileAuditStore  │ │
│  │ (Emits Spans & Logs)     │ │  (Fail-Closed L0-L4)    │ │ (.jsonl SHA256) │ │
│  └───────────┬──────────────┘ └───────────┬─────────────┘ └────────┬────────┘ │
└──────────────┼────────────────────────────┼────────────────────────┼──────────┘
               │ depends on                 │ depends on             │ depends on
               ▼                        ▼                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                                 :core-engine                                  │
│  ┌───────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐  │
│  │ ProviderConfigManager │ │   RateLimiterPort    │ │   allowed-deps.txt   │  │
│  │  & ProviderRegistry   │ │ (Token-Bucket Engine)│ │ (Vetted Allowlist)   │  │
│  └───────────────────────┘ └──────────────────────┘ └──────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Detailed Task Breakdown & Implementation Steps

### Feature Task: Provider Configuration Compose UI
1. **Compose UI Enhancement (`SettingsScreen.kt`)**:
   - Build Material 3 provider preset cards displaying provider logos, active state, and model badges.
   - Implement "Edit API Key" dialog with password visibility toggle, save action, and clear key option.
   - Implement "Test Endpoint Connection" trigger invoking `ProviderConfigurationManager.testProviderConnection()`.
2. **Provider Manager Wiring**:
   - Ensure `ProviderConfigurationManager` persists API keys to `AndroidKeyStoreManager` and provider preferences to `DataStoreStorageAdapter`.
   - Update `ProviderRegistry` to dynamically refresh `NarratorOrchestrator` upon provider edits.

### Task 1: Resolve Zero-Dependency Policy (ADR 0003)
1. **Write ADR 0003 (`docs/adr/0003-core-engine-dependencies.md`)**:
   - Record decision to adopt Option B (Vetted Allowlist).
2. **Create Allowlist File**:
   - Create `core-engine/allowed-deps.txt`.
3. **Update CI Guardrail**:
   - Replace old zero-dependency check in `.github/workflows/ci.yml` with `allowed-deps.txt` verification.

### Task 2: Scalability, Rate Limiting & Operational Degradation
1. **Concurrency & Bounds**:
   - Document threading model in `docs/scalability.md`.
2. **Rate Limiting Engine**:
   - Define `RateLimiterPort` in `:core-engine` and implement token-bucket `TokenBucketRateLimiter` in `:app`.
   - Apply rate limits outbound to providers and inbound per user session.
3. **Operational State Management**:
   - Implement state transition logic (`Normal`, `Degraded`, `Safe Mode`).
   - Create `docs/runbooks/degradation.md` and load smoke test `LoadSmokeTest.kt`.

### Task 3: Release Engineering
1. **SemVer & Versioning**:
   - Create `docs/versioning.md` and update `build.gradle.kts` for tag-derived release versions.
2. **Changelog Enforcement**:
   - Create `CHANGELOG.md` following `Keep a Changelog`. Add PR changelog guard to CI.
3. **Release Pipeline**:
   - Create `.github/workflows/release.yml` building signed APKs/JARs with Cosign signatures and checksum verification.

### Task 4: Operational Runbooks & Alerting
1. **Operational Runbooks (`docs/runbooks/`)**:
   - Create `audit-chain-broken.md`, `llm-provider-outage.md`, `telemetry-exporter-down.md`, `degradation.md`, `secret-rotation.md`, `rollback.md`.
2. **Alerting Specification**:
   - Create `docs/monitoring.md` mapping alerts to runbooks.

### Task 5: Governance & Contribution Process
1. **Community & Contribution Standards**:
   - Create `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `CODEOWNERS`, and GitHub PR/issue templates.
2. **ADR Index & Pattern Record**:
   - Create `docs/adr/README.md`, `docs/adr/0000-template.md`, and backfill `docs/adr/0001-port-pattern.md`.

---

## 5. Definition of Done Checklist (Document 03)

- [x] Provider Configuration Jetpack Compose UI implemented with preset cards, API key dialog, Base URL editor, and connection diagnostics.
- [x] `docs/adr/0003-core-engine-dependencies.md` merged and `core-engine/allowed-deps.txt` enforced in CI.
- [x] `docs/scalability.md` published and `RateLimiterPort` token-bucket engine implemented.
- [x] Operational states (`Normal`, `Degraded`, `Safe Mode`) implemented and `LoadSmokeTest.kt` passing.
- [x] `docs/versioning.md`, `CHANGELOG.md`, and CI changelog check active.
- [x] `.github/workflows/release.yml` release pipeline created.
- [x] Six operational runbooks created in `docs/runbooks/` and mapped in `docs/monitoring.md`.
- [x] `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `CODEOWNERS`, PR/issue templates, and `docs/adr/0001-port-pattern.md` created.
- [x] Full app compilation (`compile_applet`) succeeds.
