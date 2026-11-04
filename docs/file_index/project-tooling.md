# File Index — Workflows, Build Tooling and Imported Assets

Part of the [master file index](../file_index.md). 19 tracked files outside the three modules and
outside `docs/`.

## GitHub automation — `.github/`

| File | Purpose |
| :--- | :--- |
| [workflows/ci.yml](../../.github/workflows/ci.yml) | CI pipeline on pushes to `main` and `phase/**` and on pull requests to `main`: JDK 17, Android SDK setup, guardrail steps C1, C2, C3, C4, C6, C8, C10, gitleaks secret scan (C5, non-blocking), `gradle test --no-daemon`, `gradle assembleDebug --no-daemon` |
| [workflows/release.yml](../../.github/workflows/release.yml) | Tag-triggered release: verifies a matching `CHANGELOG.md` section, runs `gradle assembleDebug test`, installs cosign, writes SHA-256 files for the debug APKs, publishes a GitHub release |
| [ISSUE_TEMPLATE/bug_report.md](../../.github/ISSUE_TEMPLATE/bug_report.md) | Bug report template |
| [ISSUE_TEMPLATE/feature_request.md](../../.github/ISSUE_TEMPLATE/feature_request.md) | Feature request template |
| [pull_request_template.md](../../.github/pull_request_template.md) | Pull request checklist template |

## Gradle metadata — `gradle/`

| File | Purpose |
| :--- | :--- |
| [libs.versions.toml](../../gradle/libs.versions.toml) | Version catalog: AGP, Kotlin, Compose BOM, Firebase BOM, Room, WorkManager, OkHttp, Gson, Moshi, Coil, markdown-compose, credentials, MockWebServer, and the plugin aliases used by root and module builds |
| [wrapper/gradle-wrapper.properties](../../gradle/wrapper/gradle-wrapper.properties) | Declares Gradle 9.3.1 for the wrapper distribution; note that `gradlew` and `gradle-wrapper.jar` are not committed, so this file alone cannot run a build |

## Imported AI Studio artifacts — `.aistudio/`

| File | Purpose |
| :--- | :--- |
| [artifacts/brain/f72dd60d-a87d-47de-8dc2-56bc361144a1/implementation_plan.md](../../.aistudio/artifacts/brain/f72dd60d-a87d-47de-8dc2-56bc361144a1/implementation_plan.md) | Revised implementation plan for the missing-features build list (Perchance removal, Narrator to `AgentOrchestrator` repurposing, offline sync) |
| [artifacts/brain/f72dd60d-a87d-47de-8dc2-56bc361144a1/implementation_plan.md.metadata.json](../../.aistudio/artifacts/brain/f72dd60d-a87d-47de-8dc2-56bc361144a1/implementation_plan.md.metadata.json) | Metadata for the plan: summary, timestamp, feedback flag |

## Imported build specs — `public/assets/aistudio/`

Ten exported markdown documents that drove the current implementation. They are historical inputs,
not descriptions of the shipped state; treat `docs/` as authoritative.

| File | Topic (first heading) |
| :--- | :--- |
| [deepseek_markdown_20261103_18409c_c12c0674.md](../../public/assets/aistudio/deepseek_markdown_20261103_18409c_c12c0674.md) | Missing Features Build List and Brief Build Specs (Revised) |
| [deepseek_markdown_20261103_201f13_70b48b9b.md](../../public/assets/aistudio/deepseek_markdown_20261103_201f13_70b48b9b.md) | Project Agent Instructions (287 lines) |
| [deepseek_markdown_20261103_2873bc_5ee2131a.md](../../public/assets/aistudio/deepseek_markdown_20261103_2873bc_5ee2131a.md) | Persistent, Editable Provider Configurations & Native Clipboard Support |
| [deepseek_markdown_20261103_2dcb8c_aeebe8b5.md](../../public/assets/aistudio/deepseek_markdown_20261103_2dcb8c_aeebe8b5.md) | Missing Features Build List and Brief Build Specs |
| [deepseek_markdown_20261103_4a9b52_66d08451.md](../../public/assets/aistudio/deepseek_markdown_20261103_4a9b52_66d08451.md) | 02 — Short-Term Hardening: HTTP Resilience, Observability, Security, Integration Tests (548 lines) |
| [deepseek_markdown_20261103_7a86e6_bb445a2e.md](../../public/assets/aistudio/deepseek_markdown_20261103_7a86e6_bb445a2e.md) | 01 — Immediate Foundation: Tests, JSON Portability, Audit Persistence, CI (485 lines) |
| [deepseek_markdown_20261103_85ceb4_78ffaf33.md](../../public/assets/aistudio/deepseek_markdown_20261103_85ceb4_78ffaf33.md) | 03 — Long-Term Production Readiness: Scale, Operations, Process (514 lines) |
| [deepseek_markdown_20261104_16797c_82a1def9.md](../../public/assets/aistudio/deepseek_markdown_20261104_16797c_82a1def9.md) | Next Steps: Agent Swarm & Tool Execution Pipeline |
| [deepseek_markdown_20261104_c8dd08_579333f6.md](../../public/assets/aistudio/deepseek_markdown_20261104_c8dd08_579333f6.md) | Build Instructions: Fixing Provider Configuration, Token Limits, and Cleanup |
| [deepseek_markdown_20261104_d61d20_ec12caf5.md](../../public/assets/aistudio/deepseek_markdown_20261104_d61d20_ec12caf5.md) | Prime Push: Workspace File I/O & Self-Modification Pipeline |

Four of these exceed 200 lines (marked above). They are stored verbatim as imported inputs; the
project rule that limits authored documents to 200 lines has not been applied to them because
splitting would break provenance. Say the word and they can be chunked with cross-reference
pointers instead.
