# Master File Index

Hub for the repository file index. Every tracked path is listed exactly once across this file and its
four parts.

- **208 tracked files** in total.
- 12 root-level files, 25 documentation files, 3 modules, 2 workflow files, 10 imported AI Studio
  assets.

## Index parts

| Part | Covers | Files |
| :--- | :--- | ---: |
| [file_index/core-engine.md](file_index/core-engine.md) | `core-engine/` — Java 11 engine, ports, models, tests | 48 |
| [file_index/security-interface.md](file_index/security-interface.md) | `security-interface/` — interlock, policy, audit, tests | 16 |
| [file_index/app-module.md](file_index/app-module.md) | `app/` — source, resources, tests, fixtures | 88 |
| [file_index/project-tooling.md](file_index/project-tooling.md) | `.github/`, `gradle/`, `.aistudio/`, `public/` | 19 |
| this file | root-level files and `docs/` | 37 |

Counts are per tracked file (`git ls-files`), excluding directories and files ignored by
`.gitignore`.

---

## 1. Root configuration and documentation

| Path | Purpose |
| :--- | :--- |
| [README.md](../README.md) | Repository overview, module topology, build/test commands, provider presets, documentation map |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Local setup, guardrail rules, pull request checklist |
| [CHANGELOG.md](../CHANGELOG.md) | Release history in Keep-a-Changelog style |
| [SECURITY.md](../SECURITY.md) | Vulnerability reporting policy |
| [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) | Contributor code of conduct |
| [CODEOWNERS](../CODEOWNERS) | Review ownership per path |
| [settings.gradle.kts](../settings.gradle.kts) | Multi-module inclusion of `:app`, `:core-engine`, `:security-interface` and repository declarations |
| [build.gradle.kts](../build.gradle.kts) | Root build script: plugin aliases only |
| [gradle.properties](../gradle.properties) | JVM and AndroidX build flags |
| [metadata.json](../metadata.json) | AI Studio platform metadata (name, description, capabilities) |
| [.env.example](../.env.example) | Template consumed by the Secrets Gradle plugin |
| [.gitignore](../.gitignore) | Ignore rules for build output, local `.env` and IDE files |

## 2. Documentation (`docs/`)

| Path | Purpose |
| :--- | :--- |
| [index.md](index.md) | Documentation home; entry point for every other document |
| [architecture.md](architecture.md) | Modules, layering, ports and adapters, runtime flows, deviations |
| [status.md](status.md) | Verified implementation status, unresolved references, tooling gaps |
| [file_index.md](file_index.md) | This hub |
| [file_index/core-engine.md](file_index/core-engine.md) | File index for `:core-engine` |
| [file_index/security-interface.md](file_index/security-interface.md) | File index for `:security-interface` |
| [file_index/app-module.md](file_index/app-module.md) | File index for `:app` |
| [file_index/project-tooling.md](file_index/project-tooling.md) | File index for workflows, Gradle wrapper metadata, AI Studio assets |
| [ci.md](ci.md) | CI pipeline steps and guardrails C1–C10 |
| [testing.md](testing.md) | Test strategy and test inventory per module |
| [e2e.md](e2e.md) | End-to-end harness, fixtures, how to add a scenario |
| [networking.md](networking.md) | Transports, streaming, retries, timeouts, guardrail C6 |
| [observability.md](observability.md) | Telemetry ports, signal names, redaction, fault isolation |
| [monitoring.md](monitoring.md) | Metrics and logs to watch, current export status |
| [scalability.md](scalability.md) | Budgets, pool sizes and limits |
| [audit-format.md](audit-format.md) | Audit record schema, hash chain, rotation |
| [versioning.md](versioning.md) | Version sources and release workflow behaviour |
| [adr/README.md](adr/README.md) | ADR index |
| [adr/0000-template.md](adr/0000-template.md) | ADR template |
| [adr/0001-port-pattern.md](adr/0001-port-pattern.md) | Decision: ports and adapters boundary |
| [adr/0002-json-codec.md](adr/0002-json-codec.md) | Decision: `JsonCodecPort` and `MiniJson` |
| [adr/0003-core-engine-dependencies.md](adr/0003-core-engine-dependencies.md) | Decision: core-engine dependency policy |
| [security/threat-model.md](security/threat-model.md) | Trust boundaries, threats T1–T5, mitigations |
| [security/tool-adapters.md](security/tool-adapters.md) | Security checklist per tool adapter |
| [runbooks/](runbooks/) | Six operational runbooks, listed in [index.md](index.md) |

## 3. Modules

- [`core-engine/`](file_index/core-engine.md) — pure Java 11 engine: 14 ports, 14 models, JSON codec,
  OpenAI-compatible client, provider registry, tool registry, 6 test classes.
- [`security-interface/`](file_index/security-interface.md) — risk policy, interlock, telemetry
  interceptor, tamper-evident audit store, 5 test classes.
- [`app/`](file_index/app-module.md) — Compose UI, Room, DataStore, WorkManager, OkHttp adapters,
  10 tool workers, unit and instrumentation tests with LLM fixtures.

## 4. How to keep this index accurate

1. Add a row when you add a tracked file, in the part that owns that directory.
2. Update the count in the table above when a part grows.
3. Run `git ls-files` and compare with the parts; the totals must match.
