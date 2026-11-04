# Architectural Decision Records (ADR) Index

Canonical record of key architectural decisions for Apex Agentic Master Chat. ADRs are immutable once
accepted; a changed decision gets a new ADR that supersedes the old one.

| ADR | Title | Status | Date |
| :--- | :--- | :--- | :--- |
| [0000](0000-template.md) | ADR template | Active | 2026-11-03 |
| [0001](0001-port-pattern.md) | Hexagonal ports and adapters architecture | Accepted | 2026-11-03 |
| [0002](0002-json-codec.md) | Abstract JSON parsing behind `JsonCodecPort` | Accepted | 2026-11-03 |
| [0003](0003-core-engine-dependencies.md) | Core engine dependency governance policy | Accepted | 2026-11-03 |

Related documentation: [../architecture.md](../architecture.md) for the resulting design,
[../ci.md](../ci.md) for how guardrails C1–C4 enforce it, and
[../../core-engine/allowed-deps.txt](../../core-engine/allowed-deps.txt) for the dependency allow-list
introduced by ADR 0003.
