# Runbook: Operational State Degradation & Recovery

## Operational States Matrix
| State | Value | Triggers | System Behavior |
| :--- | :--- | :--- | :--- |
| **NORMAL** | `0` | Default baseline. | All tool risk levels (`L0`–`L4`) and LLM features active. |
| **DEGRADED** | `1` | LLM provider error rate $> 10\%$ over 5 min, or rate-limit timeouts exceed threshold. | New agent sessions rejected with error message; existing model calls retry under exponential backoff. |
| **SAFE_MODE** | `2` | Audit store disk write failure or `ApprovalPort` UI unreachable. | High-risk tools (`L2`–`L4`) immediately denied; only `L0`/`L1` read-only tools allowed. |

## Operator Actions
1. **Diagnosis**: Query metric gauge `system.state` or check structured log event `system.state_changed`.
2. **Investigation**:
   - If `SAFE_MODE`: Inspect `audit_active.jsonl` disk permissions and storage space.
   - If `DEGRADED`: Inspect network connection metrics and `ratelimit.throttled` counters.
3. **Recovery**: Once underlying issue is resolved, invoke `SystemStateManager.transitionTo(State.NORMAL, "Issue resolved")`.
