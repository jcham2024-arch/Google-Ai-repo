# Runbook: Telemetry Exporter Unreachable

## Symptoms
`OtelTelemetry` fails to export spans or metrics to collector. Logcat reports telemetry connection failures.

## Impact
Zero impact on core agent tool execution or chat generation due to `SafeTelemetryDecorator` fault isolation (Constraint C9).

## Mitigation
1. Verify collector host and port accessibility.
2. `SafeTelemetryDecorator` automatically drops failing export payloads while logging local fallback events.
3. Restart telemetry collector service once network connection is restored.
