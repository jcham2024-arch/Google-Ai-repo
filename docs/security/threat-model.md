# Threat Model

Model output and network responses are untrusted inputs. Every tool call they produce crosses a
validation boundary before it can touch device state.

```
        UNTRUSTED                          TRUSTED
  ┌────────────────────┐            ┌──────────────────────────────┐
  │ model text / tool  │  tool call │  SecurityInterlock           │
  │ calls              │───────────▶│  risk lookup (L0–L4)         │
  ├────────────────────┤            │  approval gate for L3/L4     │
  │ external HTTP      │  response  │  budget + kill switch        │
  │ responses          │───────────▶│  ToolWorker execution        │
  └────────────────────┘            │  audit append (SHA-256 chain)│
                                    └──────────────────────────────┘
```

## Enumerated threats

### T1 — Prompt injection escalating to privileged tools

- Threat: crafted prompt content convinces the model to call `run_command` or `write_file_external`.
- Mitigation: the risk decision comes from `ToolRiskRegistry`, never from the model. `L3` and `L4`
  calls require an approving `ApprovalDecision` whose request id matches the request; a mismatch, a
  null decision or a throwing approval port all deny the call.
- Verified by: `SecurityInterlockTest.testL4ToolRequiresApprovalAndExecutesWhenApproved`,
  `testL4ToolFailsWhenUserRejects`, `testUnknownToolFailsClosedToL3`,
  `SecurityInterlockFailClosedTest.testMismatchedRequestIdIsDenied`.

### T2 — Malformed tool arguments crashing execution

- Threat: the model emits calls with missing or wrong-typed arguments.
- Mitigation: the interlock rejects null/empty tool names (`DENIED_MALFORMED`) and unknown tool names
  (`ERROR_NOT_FOUND`), and every worker throws `IllegalArgumentException` for a missing required
  argument; worker exceptions become `ERROR_THROWN` results rather than crashes.
- Caveat: `ToolSchema` exists as a model, but no validation pass over arguments is performed anywhere
  in the code, so a present-but-wrong argument reaches the worker.
- Verified by: `SecurityInterlockFailClosedTest.testNullToolCallIsDenied`,
  `SecurityInterlockTest.testToolExceptionHandledGracefully`.

### T3 — Audit trail tampering

- Threat: rewriting or truncating history to hide an execution.
- Mitigation: each record commits to the previous entry hash (SHA-256 chain over
  `timestamp:toolName:riskLevel:decision:argsHash:previousHash`); `FileAuditStore.verify()` returns
  the index of the first inconsistent record and `AuditChainBrokenException` reports it. Writes are
  fsynced.
- Caveat: the chain is not signed or keyed, so an attacker who can rewrite the whole file can
  recompute a valid chain. Production code currently constructs `AuditLogManager()` without a store,
  so records are in memory only (see [../status.md](../status.md)).
- Verified by: `FileAuditStoreTest.testTamperingDetection`, `testMismatchedPrevHashThrowsException`,
  `AuditLogManagerTest.testSequentialHashChainLinking`.

### T4 — Secret exfiltration through telemetry

- Threat: API keys or tokens appear in logs or spans.
- Mitigation: `SafeTelemetryDecorator` replaces values of any attribute or log field whose key
  matches `(?i).*(token|secret|key|password).*` with `[REDACTED]`, and swallows telemetry failures so
  they cannot change control flow.
- Verified by: `SafeTelemetryDecoratorTest.testSecretRedaction`,
  `testThrowingDelegateDoesNotCrashCaller`.

### T5 — Unbounded tool loops (denial of service)

- Threat: an agent issues tool calls forever.
- Mitigation: `SecurityInterlock` enforces `DEFAULT_MAX_ITERATIONS = 10` per conversation, records
  `DENIED_BUDGET_EXHAUSTED` and throws `BudgetExhaustedException`; a kill switch
  (`toolsPaused`) denies execution with `DENIED_TOOLS_PAUSED`.
- Verified by: `SecurityInterlockFailClosedTest.testExceedingMaxIterationsThrowsBudgetExhaustedException`.

### T6 — Path traversal in file tools

- Threat: a model supplies `../../databases/app.db` to read or overwrite data outside the sandbox.
- Mitigation: `FileGovernanceEngine.validatePath` rejects absolute paths, any `..` segment, and
  canonical paths that escape the sandbox root before writing, restoring or deleting.
- Caveat: `ReadFileToolWorker` and `ReadFileRangeToolWorker` resolve `File(baseDir, path)` directly
  with no validation, so their safety depends entirely on the `baseDir` passed at construction.
  Read-tool exposure is therefore unverified.
- Verified by: no automated test covers path traversal.

## Residual risks

| Risk | Status |
| :--- | :--- |
| Device compromise (root, unlocked bootloader) | Audit files and DataStore are readable/modifiable; API keys remain Keystore-encrypted, but a root attacker can call `decrypt` through the app process |
| Unsigned audit chain | Recomputable by anyone who can rewrite the whole file |
| In-memory audit in production | No durable record between app launches |
| `run_command` | Currently returns a formatted string and executes nothing, so its `L4` gating protects a stub; when it becomes real, the risk level must stay `L4` |
| Telemetry has no durable sink | Evidence of an incident can be lost with the process |
| Debug signing key in repository | `debug.keystore` is committed; debug APKs are not trustworthy artifacts |
| Firebase credentials | AppCheck/Firestore/Auth flows are unverified in this environment |

## Not in scope

Server-side compromise of a model provider, supply-chain attacks on Gradle dependencies, and physical
attacks requiring an unlocked device with developer options are outside this model.
