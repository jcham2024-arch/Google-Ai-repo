# Monitoring

## What exists today

| Surface | Where | Status |
| :--- | :--- | :--- |
| Structured logs | `OtelTelemetry` writes spans, counters, histograms and logs to `android.util.Log` | active |
| Tool signals | `tool.invoke`, `tool.invocations`, `tool.duration_ms`, `security.denials` from `ToolInvocationInterceptor` | active when the interlock path runs |
| Rate-limit signals | `ratelimit.acquired`, `ratelimit.throttled`, `ratelimit.timeout` | implemented, but the limiter is not wired into the transport |
| Audit log | `audit_active.jsonl` plus `audit_<millis>.jsonl` archives under the configured directory | store is wired in tests only; production code constructs `AuditLogManager()` with no store, so records stay in memory |
| Offline queue depth | `OfflineQueueDao` pending-count flow consumed by `OfflineSyncManager` | active |
| Dashboards, alerting, exporters | — | none |

There is no OpenTelemetry collector, no Prometheus endpoint, no crash reporting integration, and no
alerting rule in this repository. Everything is observable only through Logcat or the audit files.

## Signals worth watching

| Signal | Interpretation | Suggested threshold |
| :--- | :--- | :--- |
| `tool.invocations` growth with flat `security.denials` | Tools are executing normally | — |
| `security.denials` spike | Kill switch, missing approver, refusals, or malformed calls | Investigate any sustained increase; see [runbooks/degradation.md](runbooks/degradation.md) |
| `tool.duration_ms` tail | Slow tool worker or blocked network call | Alert above a few seconds per invocation once metrics are exported |
| Retry-heavy transport logs | Provider throttling or an outage | See [runbooks/llm-provider-outage.md](runbooks/llm-provider-outage.md) |
| Missing audit records after an app restart | Expected today: the production audit sink is in-memory, so nothing is persisted between launches | Wire a `FileAuditStore` before treating audit as durable |
| Offline queue depth that never drains | Replay loop failing or the device stays offline | See [runbooks/degradation.md](runbooks/degradation.md) |

## How to inspect on a device

```bash
# All telemetry lines, tag prefix configured on OtelTelemetry
adb logcat -s OtelTelemetry:V

# Audit records
adb shell run-as <applicationId> cat files/audit/audit_active.jsonl
```

## Gaps to close before production

1. Export telemetry to a backend; today it never leaves the process.
2. Add provider-level metrics: request latency, retry count, token usage, error rate by status code.
3. Persist audit records in production (`AuditLogManager(FileAuditStore(...))`) and alarm on append
   failure; today a failing sink only surfaces as an exception and nothing survives a restart.
4. Wire the rate limiter into `OkHttpTransport` so throttling is measurable.
5. Define retention for rotated audit segments.
