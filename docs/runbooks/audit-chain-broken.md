# Runbook: Broken Audit Hash Chain Recovery

## Symptoms
`FileAuditStore.verify()` returns a non-null corruption index or `AuditChainBrokenException` is thrown during startup or audit verification.

## Impact
Interlock halts startup or switches system state to `SAFE_MODE`. High-risk tools (`L2`–`L4`) are blocked.

## Diagnosis
1. Read active log file: `cat audit_active.jsonl`.
2. Locate line index returned by `verify()`.
3. Check `hash` and `prev` fields for unauthorized line edits, missing sequence numbers, or disk truncation.

## Mitigation
1. Preserve corrupted audit log for forensic inspection: `cp audit_active.jsonl audit_corrupted_backup.jsonl`.
2. Rotate corrupted log: execute `FileAuditStore.rotate()`.
3. Re-initialize genesis state and resume service.
