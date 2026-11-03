# Observability, Telemetry & Fault Isolation

## Overview
Apex captures structured signals across all model calls, tool executions, and security interlock evaluations. Telemetry is governed by `TelemetryPort` in `:core-engine` and decorated with `SafeTelemetryDecorator` to guarantee zero-crash fault isolation.

## Architectural Guardrails
- **Guardrail C7**: Every tool invocation MUST emit exactly one structured log line, one metric counter, one metric histogram, and one trace span.
- **Guardrail C8**: No raw `println`, `printStackTrace`, or `System.out` calls are permitted anywhere in production source code.
- **Guardrail C9**: A failing `TelemetryPort` implementation MUST NEVER crash the core engine or stop tool execution.

## Fault Isolation (`SafeTelemetryDecorator`)
All telemetry invocations are wrapped in `SafeTelemetryDecorator`. If an OTLP exporter, logging framework, or database writer throws an unhandled exception, `SafeTelemetryDecorator` catches the exception safely and allows business logic execution to proceed unaffected.

## Secret Redaction
To prevent accidental credential leaks into telemetry stores, `SafeTelemetryDecorator` automatically redacts attributes or log fields matching sensitive key patterns:
$$\text{Regex}: \text{(?i).*(token|secret|key|password).*}$$
Matching values are replaced with `"[REDACTED]"`.

## Canonical Telemetry Signal Index

| Signal Type | Signal Name | Attributes / Tags | Description |
| :--- | :--- | :--- | :--- |
| **Span** | `tool.invoke` | `tool.id`, `outcome` | Traces tool execution lifecycle. |
| **Log** | `tool.invoked` | `tool.id`, `duration.ms`, `outcome` | Structured INFO log line for tool invocation. |
| **Counter** | `tool.invocations` | `tool.id` | Increments total tool invocation attempts. |
| **Counter** | `security.denials` | `tool.id` | Increments security or approval denials. |
| **Histogram** | `tool.duration_ms` | `tool.id` | Measures tool execution latency in milliseconds. |
