# Tamper-Evident Audit Format Specification

## Overview
Apex maintains a durable, append-only, cryptographically linked audit trail of every tool execution attempt and decision. Every record is stored as a single line of JSON (newline-delimited JSON / JSONL) in `audit_active.jsonl`.

## Invariants
1. **Durability**: Every write commits synchronously to non-volatile storage via `FileChannel.force(true)` before the method returns.
2. **Hash Chaining**: Every record's `entryHash` binds the current event state and the previous record's `entryHash`.
3. **Genesis Hash**: The first record in an empty chain links to the genesis hash:
   `0000000000000000000000000000000000000000000000000000000000000000` (64 zeros).
4. **Fail-Closed Verification**: Any discrepancy between expected and observed hashes throws `AuditChainBrokenException` or returns the exact 0-based corruption index during `verify()`.

## Record Schema
Each line in `audit_active.jsonl` adheres to the following JSON structure:

```json
{
  "seq": 0,
  "ts": 1700000000000,
  "tool": "read_file",
  "risk": "L0",
  "decision": "EXECUTED_OK",
  "argsHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "prev": "0000000000000000000000000000000000000000000000000000000000000000",
  "hash": "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"
}
```

### Field Definitions
- `seq` (integer): Monotonically increasing 0-indexed sequence counter.
- `ts` (long): Epoch timestamp in milliseconds.
- `tool` (string): Unique identifier of the requested tool.
- `risk` (string): Risk classification level (`L0`, `L1`, `L2`, `L3`, `L4`).
- `decision` (string): Execution outcome (`EXECUTED_OK`, `DENIED`, `DENIED_NO_APPROVER`, `ERROR_NOT_FOUND`, `ERROR_THROWN`).
- `argsHash` (string, 64 hex characters): SHA-256 digest of serialized arguments string.
- `prev` (string, 64 hex characters): The `hash` of the immediately preceding record, or the 64-zero genesis hash.
- `hash` (string, 64 hex characters): The canonical cryptographic digest of this entry.

## Canonical Hashing Formulas
All digests use UTF-8 string encoding and produce 64-character lowercase hexadecimal outputs.

### Arguments Digest
$$\text{argsHash} = \text{SHA-256}(\text{arguments})$$

### Entry Digest
$$\text{raw} = \text{timestamp} + \text{":"} + \text{toolName} + \text{":"} + \text{riskLevel} + \text{":"} + \text{decision} + \text{":"} + \text{argsHash} + \text{":"} + \text{previousHash}$$

$$\text{entryHash} = \text{SHA-256}(\text{raw})$$

## Rotation & Archival
When log rotation occurs (e.g. threshold reached or initiated via `FileAuditStore.rotate()`):
1. The active `FileChannel` is flushed with `channel.force(true)` and closed.
2. The active file `audit_active.jsonl` is renamed to `audit_<timestamp>.jsonl`.
3. A new `audit_active.jsonl` is initialized, preserving chain continuity.
