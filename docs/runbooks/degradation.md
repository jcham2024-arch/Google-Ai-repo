# Runbook: Operational State Degradation

## State model

`SystemStateManager.State` defines three values:

| State | Value | Intended behaviour |
| :--- | :--- | :--- |
| `NORMAL` | 0 | All risk levels and model features available |
| `DEGRADED` | 1 | New sessions rejected; in-flight calls retried under backoff |
| `SAFE_MODE` | 2 | Only `L0`/`L1` tools allowed; `L2`–`L4` denied |

Transitions are explicit: `SystemStateManager.transitionTo(state, reason)` emits a `WARN` log event
named `system.state_changed` (fields `old_state`, `new_state`, `reason`) and records the
`system.state` histogram with the numeric value.

## Important caveat

Nothing in the application constructs `SystemStateManager`, and no code path calls `transitionTo`.
The thresholds described below are the intended operating policy, not automated behaviour. Today,
degradation is a manual decision plus whatever the interlock does on its own (kill switch, budget
exhaustion, missing approver).

## Symptoms to watch

| Symptom | Likely cause | Source of evidence |
| :--- | :--- | :--- |
| Every `L3`/`L4` call denied with `DENIED_NO_APPROVER` | Approval UI unreachable, `AndroidApprovalPort` not wired | Audit decisions, `security.denials` counter |
| Provider calls failing or retrying heavily | Provider outage or throttling (429) | Transport logs, retry delays, `Retry-After` handling |
| `DENIED_BUDGET_EXHAUSTED` repeatedly | Model looping on tools | Audit decisions |
| `DENIED_TOOLS_PAUSED` | Kill switch engaged | Audit decisions |
| Offline queue depth that never falls | Device offline or replay failing | `OfflineQueueDao` pending-count flow |

## Operator actions

1. Confirm the current state: check for a `system.state_changed` log event; if none exists, the
   process is in the `NORMAL` default and has never transitioned.
2. For approval-related denials: verify the approval UI can be composed (`ChatScreen` constructs
   `AndroidApprovalPort` and `SecurityInterlock` inside the composable, so the gate is absent
   whenever that screen is not active).
3. For provider failures: follow [llm-provider-outage.md](llm-provider-outage.md).
4. For audit-sink problems: follow [audit-chain-broken.md](audit-chain-broken.md).
5. To move to `SAFE_MODE` deliberately, engage the interlock kill switch (`toolsPaused`) or call
   `transitionTo(State.SAFE_MODE, reason)` once a caller exists.
6. To recover, resolve the cause and call `transitionTo(State.NORMAL, "Issue resolved")`.
7. After recovery, verify: a tool call succeeds, its audit record appears, and no denial counter is
   still climbing.
