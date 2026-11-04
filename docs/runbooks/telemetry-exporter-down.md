# Runbook: Telemetry Export Unavailable

## Current reality

There is no exporter and no collector. `OtelTelemetry` writes spans, counters, histograms and logs to
`android.util.Log` only; `NoOpTelemetry` discards everything. Nothing ships telemetry off-device, so
"exporter down" today means "log output unavailable", for example when logcat is rotated away or the
device log buffer is full.

## Impact

No impact on tool execution, chat turns or audit records. `SafeTelemetryDecorator` swallows telemetry
failures and `ToolInvocationInterceptor` wraps its telemetry port in it, so a broken telemetry backend
cannot propagate into the execution path.

## Symptoms

| Symptom | Meaning |
| :--- | :--- |
| No `tool.invoke` span lines in logcat though tools ran | Log output suppressed, tag filter mismatched, or the telemetry implementation replaced by `NoOpTelemetry` |
| Telemetry exceptions in the debug log | A delegate threw; the decorator contained it and replaced the value with a safe default |
| `[REDACTED]` attribute values | The redaction rule matched a key containing `token`, `secret`, `key` or `password` — expected behaviour, not a fault |

## Actions

1. Confirm the implementation in use: `OtelTelemetry` (Logcat) is the only non-null implementation in
   the app; `NoOpTelemetry` is the null object.
2. Check the log tag prefix passed to `OtelTelemetry` and filter accordingly, for example
   `adb logcat -s OtelTelemetry:V`.
3. If logs are missing due to buffer pressure, capture immediately after reproducing, or raise the
   logcat buffer size on the device.
4. Treat telemetry loss as non-blocking. The durable record of security-relevant decisions is the
   audit chain, not telemetry (see [audit-format.md](../audit-format.md)), and audit persistence is
   itself not yet wired in production.

## If a collector is added later

1. Wrap the exporter in `SafeTelemetryDecorator` so an unreachable collector cannot break execution.
2. Keep a bounded in-memory buffer with a drop counter instead of blocking the caller.
3. Alert on the drop counter, not on exporter errors, to avoid alert storms during a collector
   outage.
4. Never send raw argument values or secrets; the redaction rule only guards attribute keys.
