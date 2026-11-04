# Audit Log Format

Implemented in `security-interface/src/main/java/com/apex/security/audit/`:
`AuditLogManager` builds and verifies records, `FileAuditStore` persists them.

## Storage layout

| Item | Value |
| :--- | :--- |
| Record encoding | One JSON object per line (JSONL), UTF-8 |
| Active file | `audit_active.jsonl` inside the configured directory |
| Archived file | `audit_<epochMillis>.jsonl` produced by `FileAuditStore.rotate()` |
| Durability | Every append and rotation calls `FileChannel.force(true)` (fsync) |
| JSON writing | Hand-rolled escaping in `AuditLogManager` (control characters become `\uXXXX`) |

## Record schema

`com.apex.core.port.model.AuditRecord` has seven fields, all written in this order:

| Field | Type | Meaning |
| :--- | :--- | :--- |
| `timestamp` | long | Epoch milliseconds at decision time |
| `toolName` | string | Tool id; `unknown` or `invalid_call` for malformed calls |
| `riskLevel` | string | `L0`–`L4` from `ToolRiskRegistry` |
| `decision` | string | Outcome code (see below) |
| `argumentsHash` | string | SHA-256 of the canonicalised argument map |
| `previousHash` | string | `entryHash` of the preceding record, or the genesis hash |
| `entryHash` | string | SHA-256 of this record's chain input |

Null fields are coerced to empty strings by the model constructor.

## Hash chain

- Genesis hash: 64 zero characters
  (`AuditLogManager.GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"`).
- Argument hash: `sha256(canonicalizeArguments(arguments))`, where arguments are ordered through a
  `TreeMap` so key order does not change the hash.
- Chain input: `timestamp + ":" + toolName + ":" + riskLevel + ":" + decision + ":" + argsHash + ":" + previousHash`.
- Entry hash: `sha256(chain input)`, produced with `MessageDigest.getInstance("SHA-256")`.

Raw argument values are never stored; only the hash is. Two identical argument maps under the same
preceding hash therefore produce identical records.

## Decision codes

Emitted by `SecurityInterlock`:

| Code | Trigger |
| :--- | :--- |
| `DENIED_TOOLS_PAUSED` | Kill switch active; execution paused |
| `DENIED_BUDGET_EXHAUSTED` | Iteration budget exceeded (default 10) |
| `DENIED_MALFORMED` | Null or empty tool call / tool name |
| `DENIED_NO_APPROVER` | Risk requires approval but no `ApprovalPort` is configured |
| `DENIED_APPROVER_EXCEPTION` | The approval port threw |
| `DENIED` | Approver refused the call |
| `DENIED_REQUEST_ID_MISMATCH` | Approval decision belongs to a different request id |
| `ERROR_NOT_FOUND` | No `ToolWorker` registered under the tool name |
| `EXECUTED_OK` | Worker ran and returned output |
| `ERROR_THROWN` | Worker threw; the exception message is returned to the caller |

## Verification

`AuditLogManager.verifyChain()` returns `true` when the whole chain validates. `FileAuditStore.verify()`
returns the zero-based index of the first record whose `previousHash` does not match the accumulated
hash, or `null` when the chain is intact. `AuditChainBrokenException` carries that index when thrown.

When no store is supplied, `AuditLogManager` keeps an in-memory list, so the chain can be built and
verified without touching disk — that is how the unit tests exercise it.

## Known limitations

0. Production wiring is in-memory only: `ChatScreen.kt:819` constructs `AuditLogManager()` without an
   `AuditStorePort`, so the chain lives and dies with the process. `FileAuditStore` is exercised by
   `AgentPipelineE2ETest` and `FileAuditStoreTest`, not by application code. Treat the file layout
   above as the intended durable format rather than the current runtime behaviour.
1. `SecurityInterlock.java:91,103,109` passes a `String` message where
   `createAndAppend(long, String, String, String, Map<String, String>)` expects the argument map, so
   those three call sites do not compile. The audit format above describes the intended, and for the
   other seven call sites the actual, behaviour.
2. There is no retention policy beyond manual rotation, and no signature or MAC over the chain; an
   attacker who can rewrite every record can recompute a valid chain.
3. `argumentsHash` hides values but does not prevent dictionary attacks on low-entropy arguments.
