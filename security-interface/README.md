# `:security-interface` — Interlock, Risk Policy and Audit

Android library (`namespace = "com.apex.security"`, `minSdk 24`, `compileSdk 36`, Java 11) that
depends on `:core-engine` and `androidx.core.ktx`. It provides the gate between model-requested tool
calls and device state.

## Risk policy

`RiskLevel` defines `L0`–`L4` and `requiresApproval()` returns true for `L3` and `L4`.

`ToolRiskRegistry` seeds these mappings and resolves anything else to `L3`:

| Tool | Level |
| :--- | :--- |
| `read_file` | L2 |
| `list_files` | L2 |
| `http_get` | L2 |
| `write_file` | L3 |
| `write_file_external` | L3 |
| `http_post` | L3 |
| `run_command` | L4 |
| unregistered name, or null | L3 |

`SystemStateManager` tracks `NORMAL` (0), `DEGRADED` (1) and `SAFE_MODE` (2), emitting a
`system.state_changed` log and a `system.state` histogram on transition. No application code
constructs it yet, so transitions are currently manual only.

## `SecurityInterlock`

Implements `ToolExecutorPort`. Order of operations per call:

1. Kill switch check (`toolsPaused`) → deny with `DENIED_TOOLS_PAUSED`.
2. Iteration budget check, `DEFAULT_MAX_ITERATIONS = 10` → deny with `DENIED_BUDGET_EXHAUSTED` and
   throw `BudgetExhaustedException`.
3. Malformed call check → deny with `DENIED_MALFORMED`.
4. Risk lookup; if approval is required, request it through `ApprovalPort`. Missing port, thrown
   exception, null/denied decision or request-id mismatch all deny.
5. Tool lookup in `ToolRegistry`; unknown names are denied with `ERROR_NOT_FOUND`.
6. Execute the worker; success appends `EXECUTED_OK`, a thrown exception appends `ERROR_THROWN` and
   returns a failure result.

All paths fail closed and every path appends an audit record.

## `ToolInvocationInterceptor`

Implements `ToolExecutorPort` as a decorator around another executor. It opens a `tool.invoke` span
with `tool.id`, counts `tool.invocations` and `security.denials`, records `tool.duration_ms`, records
exceptions on the span, and wraps its telemetry in `SafeTelemetryDecorator`.

## Audit

- `AuditLogManager` implements `AuditPort`: builds records, links them with SHA-256, exposes
  `verifyChain()`, and falls back to an in-memory list when no `AuditStorePort` is supplied.
- `FileAuditStore` implements `AuditStorePort`: JSONL segments (`audit_active.jsonl`, rotated to
  `audit_<millis>.jsonl`), fsync on append and rotation, `verify()` returning the zero-based index of
  the first broken link.
- `AuditChainBrokenException` carries the failing index.
- Format details and decision codes: [../docs/audit-format.md](../docs/audit-format.md).

## Tests

Five classes, 19 test methods: `AuditLogManagerTest`, `FileAuditStoreTest`,
`SecurityInterlockFailClosedTest`, `ToolInvocationInterceptorTest`, `SecurityInterlockTest`.

```bash
gradle :security-interface:test
```

## Known issues

- `SecurityInterlock.java:91`, `:103` and `:109` pass a `String` message to
  `AuditLogManager.createAndAppend(long, String, String, String, Map<String, String>)`, which expects
  the argument map. Those three paths do not compile until the signature or the call sites are
  reconciled.
- Production code (`ChatScreen.kt:819`) constructs `AuditLogManager()` without a store, so audit
  records are in memory only and no file is written at runtime.
- The audit chain is unsigned: a full rewrite cannot be detected from inside the app.
