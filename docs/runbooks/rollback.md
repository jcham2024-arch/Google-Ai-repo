# Runbook: Release Rollback

## Symptoms
Critical regression or vulnerability discovered in recent release tag.

## Impact
Degraded app stability or security vulnerability.

## Mitigation
1. Identify last stable release tag (e.g. `v0.1.0`).
2. Revert breaking commit on `main` branch.
3. Push patch release tag (e.g. `v0.1.1`).
4. Release workflow automatically compiles, signs, and publishes patch release APKs.
5. `FileAuditStore` maintains append-only hash chain integrity across application updates.
