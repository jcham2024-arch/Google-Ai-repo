# File Index — `:security-interface`

Part of the [master file index](../file_index.md). 16 tracked files.

Module root: [`security-interface/build.gradle.kts`](../../security-interface/build.gradle.kts)
(`com.android.library`, namespace `com.apex.security`, `minSdk 24`, `compileSdk 36`, Java 11;
depends on `project(":core-engine")` and `androidx.core.ktx`),
[`security-interface/README.md`](../../security-interface/README.md).

## Audit — `src/main/java/com/apex/security/audit/`

| File | Purpose |
| :--- | :--- |
| [AuditLogManager.java](../../security-interface/src/main/java/com/apex/security/audit/AuditLogManager.java) | Builds entries, computes the SHA-256 chain, verifies it, reports the first corrupt index, rotates segments |
| [FileAuditStore.java](../../security-interface/src/main/java/com/apex/security/audit/FileAuditStore.java) | JSONL persistence of audit records with force-flush durability and segment rotation |
| [AuditChainBrokenException.java](../../security-interface/src/main/java/com/apex/security/audit/AuditChainBrokenException.java) | Signals a broken chain, carrying the failing record index |

## Interlock — `src/main/java/com/apex/security/interlock/`

| File | Purpose |
| :--- | :--- |
| [SecurityInterlock.java](../../security-interface/src/main/java/com/apex/security/interlock/SecurityInterlock.java) | `ToolExecutorPort` implementation: risk lookup, approval gate, fail-closed handling, audit append, iteration budget via `BudgetExhaustedException` |
| [ToolInvocationInterceptor.java](../../security-interface/src/main/java/com/apex/security/interlock/ToolInvocationInterceptor.java) | `ToolExecutorPort` decorator emitting per-call spans, counters and structured logs through `SafeTelemetryDecorator` |

## Policy — `src/main/java/com/apex/security/policy/`

| File | Purpose |
| :--- | :--- |
| [RiskLevel.java](../../security-interface/src/main/java/com/apex/security/policy/RiskLevel.java) | `L0`–`L4` enum with `requiresApproval()` true for `L3` and `L4` |
| [ToolRiskRegistry.java](../../security-interface/src/main/java/com/apex/security/policy/ToolRiskRegistry.java) | Thread-safe tool name to risk level map; seeded defaults, unknown tools resolve to `L3` |
| [SystemStateManager.java](../../security-interface/src/main/java/com/apex/security/policy/SystemStateManager.java) | Tracks the system state the interlock uses when deciding to fail closed |
| [BudgetExhaustedException.java](../../security-interface/src/main/java/com/apex/security/policy/BudgetExhaustedException.java) | Raised when an execution budget is consumed (default 10 iterations in the interlock) |

## Tests — `src/test/java/com/apex/security/`

| File | Covers |
| :--- | :--- |
| [audit/AuditLogManagerTest.java](../../security-interface/src/test/java/com/apex/security/audit/AuditLogManagerTest.java) | Chain construction, verification and tamper detection |
| [audit/FileAuditStoreTest.java](../../security-interface/src/test/java/com/apex/security/audit/FileAuditStoreTest.java) | Record persistence, rotation and durability behaviour |
| [interlock/SecurityInterlockFailClosedTest.java](../../security-interface/src/test/java/com/apex/security/interlock/SecurityInterlockFailClosedTest.java) | Fail-closed paths: null calls, thrown exceptions, mismatched request ids |
| [interlock/ToolInvocationInterceptorTest.java](../../security-interface/src/test/java/com/apex/security/interlock/ToolInvocationInterceptorTest.java) | Span attributes, counters and exception recording |
| [policy/SecurityInterlockTest.java](../../security-interface/src/test/java/com/apex/security/policy/SecurityInterlockTest.java) | Risk classification and approval gating behaviour |

19 test methods across these 5 classes. See [../testing.md](../testing.md) and
[../security/threat-model.md](../security/threat-model.md).
