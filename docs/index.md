# Apex Agentic Master Chat — Documentation Home

Entry point for project documentation. Every link below resolves to a file in this repository.

## Start here

- [README](../README.md) — overview, module topology, build and test commands, provider presets.
- [Implementation Status](status.md) — what is implemented, wired, stubbed, or currently unresolved.
- [Master File Index](file_index.md) — hub for the complete file index, split per module.

## Architecture

- [System Architecture](architecture.md) — modules, layering rules, ports and adapters, runtime flows.
- [ADR index](adr/README.md) — architecture decision records.
  - [ADR 0001 — Port pattern](adr/0001-port-pattern.md)
  - [ADR 0002 — JSON codec](adr/0002-json-codec.md)
  - [ADR 0003 — core-engine dependencies](adr/0003-core-engine-dependencies.md)
  - [ADR template](adr/0000-template.md)

## File index parts

- [core-engine](file_index/core-engine.md) — 48 files: ports, models, client, provider, tool registry, tests.
- [security-interface](file_index/security-interface.md) — 16 files: interlock, policy, audit, tests.
- [app module](file_index/app-module.md) — 88 files: source, resources, tests, fixtures.
- [Workflows and tooling](file_index/project-tooling.md) — 19 files: CI, version catalog, imported assets.

## Module references

- [:core-engine](../core-engine/README.md) — pure Java 11 engine: 14 ports, 14 models, OpenAI-compatible client, tool registry.
- [:security-interface](../security-interface/README.md) — interlock, L0–L4 risk policy, tamper-evident audit store.
- [:app](../app/README.md) — Compose UI, Room, DataStore, WorkManager, provider configuration.

## Engineering

- [CI Pipeline and Guardrails](ci.md) — what `ci.yml` actually enforces (C1–C10).
- [Testing Strategy](testing.md) — test inventory per module and how to run it.
- [E2E Integration Guide](e2e.md) — MockWebServer/Robolectric harness and LLM fixtures.
- [Versioning and Releases](versioning.md) — version sources and release workflow behaviour.

## Operations

- [Networking](networking.md) — transports, SSE streaming, retries, timeouts.
- [Observability](observability.md) — telemetry ports, signal names, redaction, fault isolation.
- [Monitoring](monitoring.md) — metrics and logs to watch, and current export status.
- [Scalability](scalability.md) — budgets, pool sizes, limits.
- [Audit Log Format](audit-format.md) — record schema, hash chain, rotation.
- [Runbook: Degradation](runbooks/degradation.md)
- [Runbook: LLM Provider Outage](runbooks/llm-provider-outage.md)
- [Runbook: Audit Chain Broken](runbooks/audit-chain-broken.md)
- [Runbook: Telemetry Exporter Down](runbooks/telemetry-exporter-down.md)
- [Runbook: Secret Rotation](runbooks/secret-rotation.md)
- [Runbook: Rollback](runbooks/rollback.md)

## Security

- [Threat Model](security/threat-model.md) — trust boundaries, threats T1–T5, mitigations.
- [Tool Adapter Security Checklist](security/tool-adapters.md) — risk classification per tool.

## Contributing

- [CONTRIBUTING](../CONTRIBUTING.md) — environment setup, guardrail rules, pull request checklist.
- [CHANGELOG](../CHANGELOG.md) — release history.
- [SECURITY](../SECURITY.md) — vulnerability reporting.
- [Code of Conduct](../CODE_OF_CONDUCT.md)
