# Threat Model & Security Architecture

## Overview
This document defines the threat model for Apex Agent Management Chat. Model outputs and external inputs are treated as untrusted data boundaries.

## Trust Boundaries Diagram
```
┌─────────────────────────────────────────────────────────────────┐
│                       UNTRUSTED ZONE                            │
│  ┌───────────────────────┐         ┌─────────────────────────┐  │
│  │ Model Generated Text  │         │ External Network Stream │  │
│  └───────────┬───────────┘         └────────────┬────────────┘  │
└──────────────┼──────────────────────────────────┼───────────────┘
               │ Tool Calls                       │ HTTP Responses
               ▼                                  ▼
═══════════════╧══════════════════════════════════╧═════════════════ TRUST BOUNDARY
               │ Validation & Risk Interlock Gate
               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         TRUSTED ZONE                            │
│  ┌───────────────────────┐         ┌─────────────────────────┐  │
│  │   SecurityInterlock   │ ──────► │ Cryptographic Audit Log │  │
│  │  (Fail-Closed L0-L4)  │         │  (.jsonl SHA256 Chain)  │  │
│  └───────────┬───────────┘         └─────────────────────────┘  │
│              │ Executed Tool                                    │
│              ▼                                                  │
│  ┌───────────────────────┐                                      │
│  │   In-Process Tool     │                                      │
│  └───────────────────────┘                                      │
└─────────────────────────────────────────────────────────────────┘
```

## Enumerated Threats & Mitigations

### T1: Prompt Injection leading to Unauthorized Tool Escalation
- **Threat**: Adversarial prompt content attempts to trick the model into executing high-risk tools (`run_command`, `write_file_external`).
- **Mitigation**: `SecurityInterlock` evaluates `RiskLevel` independently of model claims. High-risk tools (`L3` and `L4`) require explicit user confirmation via `ApprovalPort`.
- **Test**: `SecurityInterlockTest.testL4ToolFailsWhenUserRejects()`.

### T2: Malicious Argument Injection into Tool Inputs
- **Threat**: Model outputs argument payloads containing malformed or missing fields to trigger unhandled tool crashes.
- **Mitigation**: `ToolSchema` mandates required parameter presence before tool execution. Malformed payloads are rejected immediately.
- **Test**: `SecurityInterlockFailClosedTest.testNullToolCallIsDenied()`.

### T3: Audit Trail Tampering or Record Truncation
- **Threat**: An attacker modifies historical audit records on disk to conceal unauthorized tool calls.
- **Mitigation**: Every record is cryptographically bound using SHA-256 hash chaining against the preceding entry and `FileChannel.force(true)` fsync. Any discrepancy breaks `verify()`.
- **Test**: `FileAuditStoreTest.testTamperingDetection()`.

### T4: Secret Exfiltration via Telemetry or Logs
- **Threat**: API keys, bearer tokens, or user passwords leak into log files or telemetry streams.
- **Mitigation**: `SafeTelemetryDecorator` redacts key names matching `(?i).*(token|secret|key|password).*`.
- **Test**: `SafeTelemetryDecoratorTest.testSecretRedaction()`.

### T5: Denial of Service via Unbounded Tool Execution Loops
- **Threat**: An agent loops infinitely issuing tool calls.
- **Mitigation**: `SecurityInterlock` enforces `maxToolIterations` (default 10). Exceeding the limit throws `BudgetExhaustedException`.
- **Test**: `SecurityInterlockFailClosedTest.testExceedingMaxIterationsThrowsBudgetExhaustedException()`.

## Residual Risks
- **Hardware Compromise**: Root access to physical device storage permits raw file modification. Mitigated by hardware-backed Android KeyStore encryption for API keys.
