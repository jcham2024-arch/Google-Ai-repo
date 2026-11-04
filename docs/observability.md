# Observability

## Ports

| Port | Implementations | Notes |
| :--- | :--- | :--- |
| `TelemetryPort` | `OtelTelemetry` (`:app`, Logcat-backed, inner `OtelSpan`), `NoOpTelemetry`, `SafeTelemetryDecorator` | Spans, counters, histograms and structured logs; `TelemetryPort.Span` is an `AutoCloseable` nested interface |
| `AuditPort` | `AuditLogManager` | Records tool decisions into a hash-chained log; production code passes no store, so it uses the in-memory fallback |
| `AuditStorePort` | `FileAuditStore` | Persists and verifies the audit log; referenced only by tests today |

There is no OpenTelemetry exporter in the project: `OtelTelemetry` writes to `android.util.Log` with a
tag prefix. Nothing currently ships telemetry off-device.

## Signal names emitted in code

| Signal | Type | Source | Attributes |
| :--- | :--- | :--- | :--- |
| `tool.invoke` | span | `ToolInvocationInterceptor` | `tool.id` |
| `tool.invocations` | counter | `ToolInvocationInterceptor` | `tool.id` |
| `tool.duration_ms` | histogram | `ToolInvocationInterceptor` | `tool.id` |
| `security.denials` | counter | `ToolInvocationInterceptor` | `tool.id` |
| `ratelimit.acquired` | counter | `TokenBucketRateLimiter` | `bucket` |
| `ratelimit.throttled` | counter | `TokenBucketRateLimiter` | `bucket` |
| `ratelimit.timeout` | counter | `TokenBucketRateLimiter` | `bucket` |

Counters and histograms are only emitted by code that runs; `TokenBucketRateLimiter` is not wired into
the transport, so rate-limit signals do not appear in normal operation.

## Fault isolation and redaction

`SafeTelemetryDecorator` wraps any delegate `TelemetryPort`:

- every delegate call is guarded; a throwing telemetry backend never propagates into the tool path;
- attribute and log keys matching the case-insensitive pattern `.*(token|secret|key|password).*` have
  their values replaced with `[REDACTED]` before emission or attachment.

`ToolInvocationInterceptor` wraps its injected `TelemetryPort` in `SafeTelemetryDecorator`, so tool
invocation telemetry cannot break execution (guardrail C9 in code, not in CI).

## Logging conventions

- `OtelTelemetry` prefixes log tags with the configured tag prefix.
- Raw `println`, `printStackTrace` and `System.out` are forbidden in production source and checked by
  guardrail C8.
- Audit records are the durable record of security-relevant decisions; telemetry is in-memory/log
  only.

## What to watch when debugging

1. `tool.invoke` span with a `tool.id` attribute: the tool actually reached the interceptor.
2. `security.denials` counter: approvals were refused, an approver was missing, or the call was
   malformed.
3. Audit chain verification (`AuditLogManager.verifyChain()`) when a decision is disputed; see
   [audit-format.md](audit-format.md).
4. Logcat tags for `OtelTelemetry` output when no other sink is configured.

## Gaps

- No backend exporter, no sampling policy, no cardinality limits.
- No span for the LLM request itself; only tool invocations are instrumented.
- No metrics for provider latency, retry counts, or token usage.
- Verification of these signals is manual, via Logcat and unit tests; there is no CI assertion on the
  emitted signal names.
