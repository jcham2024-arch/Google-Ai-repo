# Continuous Integration & Architectural Guardrails

## Overview
The Apex CI pipeline (`.github/workflows/ci.yml`) runs on GitHub Actions on all pushes to `main`, `phase/**` branches, and Pull Requests targeting `main`.

In addition to building artifacts and executing test suites, the CI pipeline enforces automated **architectural guardrails** to prevent layer erosion, dependency leaks, and accidental security regressions.

## Automated Guardrail Checks

### Guardrail C1: Zero Runtime Dependencies in :core-engine
- **Rule**: `:core-engine` runtime classpath must consist strictly of `java.base`.
- **Enforcement**: Checks `core-engine/build.gradle.kts` for any `implementation(...)` entries.
- **Remediation**: If an external capability is needed in the core engine, introduce a Port interface in `com.apex.core.port` and implement an Adapter in `:security-interface` or `:app`.

### Guardrail C2: No Android Framework Pollution in :core-engine
- **Rule**: `:core-engine` must remain portable Java 11 with zero Android SDK imports (`android.*`).
- **Enforcement**: Scans `core-engine/src/main/` for `import android.`.
- **Remediation**: Remove any Android imports from `:core-engine`. Any Android-specific integrations belong in `:app` or `:security-interface`.

### Guardrail C3: Strict One-Way Layer Hierarchy
- **Rule**: Dependency direction is strictly `:app` $\rightarrow$ `:security-interface` $\rightarrow$ `:core-engine`.
- **Enforcement**: Verifies that neither `core-engine` nor `security-interface` references `project(":app")`, and `core-engine` does not reference `project(":security-interface")`.
- **Remediation**: Never introduce circular or reverse dependencies. Use ports and inversion of control.

### Guardrail C4: No Direct MiniJson Usage Outside :core-engine
- **Rule**: Production application code in `:app` must rely on `JsonCodecPort` (e.g., `OrgJsonCodec`), not the fallback `MiniJson`.
- **Enforcement**: Scans `app/src/main/` for direct imports of `com.apex.core.json.MiniJson`.
- **Remediation**: Inject `JsonCodecPort` via the application layer composition root.

### Guardrail C5: Automated Secret Scanning
- **Rule**: No unencrypted API keys, secrets, or private credentials committed to source control.
- **Enforcement**: Runs `gitleaks` against repository commit history.
- **Remediation**: Secrets must be injected via Android KeyStore, environment variables, or AI Studio Secrets panel.

## Pipeline Steps
1. Checkout repository with full git history.
2. Setup JDK 17 (Temurin) and Android SDK.
3. Execute Guardrail checks C1 through C4.
4. Execute Secret Scan (Guardrail C5).
5. Run unit test suites (`gradle test`).
6. Build debug APKs (`gradle assembleDebug`).
