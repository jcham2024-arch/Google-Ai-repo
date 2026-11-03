# ADR 0003: Core Engine Dependency Governance Policy

## Status
Accepted

## Context
The `:core-engine` module is the headless core of the Apex platform. Early development followed an unwritten "zero third-party runtime dependencies" policy (`java.base` only). While this policy guaranteed portability across Android ART and desktop JVMs, it forced the reimplementation of standard utilities (e.g. JSON encoders, HTTP clients, and cryptographic helpers), introducing maintenance tax and bug surface.

## Decision
Adopt **Option B: Vetted Allowlist Policy**.

Third-party runtime dependencies may be introduced into `:core-engine` **strictly if** they satisfy all of the following criteria:

1. **Permissive Open Source License**: Published under Apache-2.0, MIT, or BSD.
2. **Zero Transitive Runtime Dependencies**: No unvetted indirect dependencies introduced into the runtime classpath (verified via `gradle :core-engine:dependencies`).
3. **Maturity & Maintenance History**: $\ge 1.0.0$ stable release with $\ge 2$ years of maintained history.
4. **Security & Supply-Chain Clearance**: Clean `gitleaks` scan, OSSF Scorecard rating $\ge 7.0$, and recorded entry in `core-engine/allowed-deps.txt`.

Any addition to `core-engine/allowed-deps.txt` requires an explicit ADR amendment and approval from the module codeowner.

## Consequences
### Positive
- Removes reimplementation tax for standard utilities while preserving supply-chain auditing.
- Automated CI enforcement prevents accidental dependency leaks into core.
- Portability across Android ART (API 24+) and JVM 11+ is preserved.

### Negative
- Requires maintaining `core-engine/allowed-deps.txt`.
- Minor overhead for reviewing dependency additions.
