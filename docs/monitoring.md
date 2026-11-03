# Monitoring & Alerting Specification

## Canonical Alert Index

| Alert Name | Metric / Query | Threshold | Severity | Runbook Link |
| :--- | :--- | :--- | :--- | :--- |
| **AuditChainBroken** | `fileAuditStore.verify() != null` | $> 0$ broken links | **CRITICAL** | [[runbooks/audit-chain-broken.md\|audit-chain-broken.md]] |
| **SystemStateDegraded** | `system.state` gauge | State $== 1$ (`DEGRADED`) | **WARN** | [[runbooks/degradation.md\|degradation.md]] |
| **SystemStateSafeMode** | `system.state` gauge | State $== 2$ (`SAFE_MODE`) | **CRITICAL** | [[runbooks/degradation.md\|degradation.md]] |
| **ToolDenialRateHigh** | `security.denials` counter | $> 10$ denials / 5 min | **WARN** | [[security/threat-model.md\|threat-model.md]] |
| **TelemetryExportFailing** | `OtelTelemetry` error log count | $> 5$ failures / min | **WARN** | [[runbooks/telemetry-exporter-down.md\|telemetry-exporter-down.md]] |
| **RateLimitTimeoutHigh** | `ratelimit.timeout` counter | $> 5$ timeouts / min | **WARN** | [[scalability.md\|scalability.md]] |
