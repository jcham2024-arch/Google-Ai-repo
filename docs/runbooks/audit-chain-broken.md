# Runbook: Broken Audit Hash Chain

## Symptoms

- `FileAuditStore.verify()` returns a non-null integer instead of `null`.
- `AuditChainBrokenException` is thrown during verification, carrying a record index.
- `AuditLogManager.verifyChain()` returns `false`.

## Impact

The audit trail can no longer prove that recorded tool decisions are the ones that happened. Any tool
execution covered by the suspect range is unverifiable. The interlock does not currently react to this
automatically: nothing calls `verify()` in application code, and there is no automatic `SAFE_MODE`
switch. Treat a detected break as a manual stop condition.

## Diagnosis

1. Locate the active segment: `audit_active.jsonl` in the configured audit directory, plus archived
   `audit_<epochMillis>.jsonl` segments.
2. The reported index is **zero-based** and identifies the first record whose `previousHash` does not
   equal the accumulated hash of the preceding record.
3. Inspect that record and the one before it: compare `entryHash` of record *n-1* with `previousHash`
   of record *n*. Field order in each JSON line is `timestamp`, `toolName`, `riskLevel`, `decision`,
   `argumentsHash`, `previousHash`, `entryHash`.
4. Check for truncation (missing trailing lines), manual edits, byte-level corruption, or a restored
   backup that dropped the last record.
5. Remember the production caveat: application code currently constructs `AuditLogManager()` without
   an `AuditStorePort`, so a break observed in the running app can only come from an in-memory chain;
   file-level breaks come from tests or from a future wired store.

## Mitigation

1. Preserve evidence before touching anything:
   `cp audit_active.jsonl audit_corrupted_backup_<timestamp>.jsonl`.
2. Record the failing index, the two hashes, and the timestamps of surrounding records in the incident
   log.
3. Rotate the segment so new records start a fresh chain: call `FileAuditStore.rotate()`, which
   fsyncs and renames the active file to `audit_<epochMillis>.jsonl`.
4. Restart the process so `AuditLogManager` re-reads the last valid `entryHash`; a fresh manager
   starts from the genesis hash (`64` zeros) when the store is empty.
5. Do not delete or edit the corrupted segment. It is the only evidence of what happened.
6. If a key or token was involved in the affected window, rotate it as well
   (see [secret-rotation.md](secret-rotation.md)).

## Prevention

- Persist audit records in production and verify the chain at startup.
- Alarm on verification failure; today nothing does.
- Keep audit segments off-device once exported; local files are protected only by app sandboxing,
  and the chain is unsigned, so a full rewrite cannot be detected from inside the app.
