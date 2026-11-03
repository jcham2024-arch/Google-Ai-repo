# `:security-interface` — Security Interlock & Audit Relay

The `:security-interface` module acts as an enforced security boundary between user-facing interfaces and the agentic execution engine. It intercepts every tool call, classifies its risk, requests human authorization when necessary, and maintains a tamper-evident audit log.

---

## Architecture Overview

```
                      ┌──────────────────────┐
                      │   Tool Call Event    │
                      └──────────┬───────────┘
                                 │
                                 ▼
                     ┌────────────────────────┐
                     │   ToolRiskRegistry     │
                     │  (Evaluate Risk Level) │
                     └──────────┬─────────────┘
                                │
               ┌────────────────┴────────────────┐
               ▼                                 ▼
        Risk < L3 (Safe)                 Risk >= L3 (Sensitive)
               │                                 │
               │                                 ▼
               │                     ┌────────────────────────┐
               │                     │      ApprovalPort      │
               │                     │ (Prompt User in GUI)   │
               │                     └──────────┬─────────────┘
               │                                │
               │                       ┌────────┴────────┐
               │                       ▼                 ▼
               │                   Approved            Denied
               │                       │                 │
               ▼                       ▼                 ▼
     ┌───────────────────┐    ┌──────────────────┐  ┌───────────────┐
     │ Tool Execution    │    │ Tool Execution   │  │ Return Error  │
     │ in ToolRegistry   │    │ in ToolRegistry  │  │ to ReAct loop │
     └─────────┬─────────┘    └────────┬─────────┘  └───────┬───────┘
               │                       │                    │
               └───────────────────────┼────────────────────┘
                                       │
                                       ▼
                       ┌──────────────────────────────┐
                       │       AuditLogManager        │
                       │ (Append SHA-256 Hash-Chained)│
                       └──────────────────────────────┘
```

---

## Risk Level Hierarchy (`RiskLevel.java`)

Each tool is assigned a risk level by `ToolRiskRegistry`. Tools classified as `L3` or `L4` require human authorization before execution.

| Level | Classification | Default Tools | Approval Policy |
| :--- | :--- | :--- | :--- |
| **`L0`** | Read-only inspection | `read_file`, `list_files` | **Automatic** (No prompt) |
| **`L1`** | Sandboxed local write | `write_file` (internal workspace) | **Automatic** (No prompt) |
| **`L2`** | External read / HTTP GET | `write_file_external`, `http_get` | **Automatic** (No prompt) |
| **`L3`** | External mutation / Unknown | `http_post` to arbitrary hosts, **Any unknown tool** | **Requires User Approval** |
| **`L4`** | Critical system execution | `run_command`, OS operations | **Mandatory User Approval** |

> **Important**: Any tool not explicitly registered in `ToolRiskRegistry` automatically defaults to **`L3`**, ensuring a fail-safe security posture.

---

## Tamper-Evident Cryptographic Audit Trail (`AuditLogManager.java`)

Every tool evaluation, execution, and rejection is recorded into a hash-chained audit log.

### Hash Calculation Formula:
```
entryHash = SHA-256(timestamp + ":" + toolName + ":" + riskLevel + ":" + decision + ":" + argsHash + ":" + previousHash)
```

1. **`previousHash`**: The SHA-256 hash of the immediately preceding audit record. The initial record uses an all-zero genesis hash.
2. **`argsHash`**: SHA-256 hash of the tool's input arguments map.
3. **`entryHash`**: Canonical SHA-256 hash binding all fields together.
4. **Verification (`verifyChain()`)**: Traverses the audit trail and verifies that every link's hash matches the recomputed digest. If any record has been modified or excised, verification fails.

---

## Interlock Implementation (`SecurityInterlock.java`)

`SecurityInterlock` implements `ToolExecutorPort`:

1. Queries `ToolRiskRegistry` for the tool's risk level.
2. If `risk.requiresApproval()`, calls the host application's `ApprovalPort`.
3. If denied, creates an `AuditRecord` with status `DENIED` and returns an `ExecutionResult.fail` without calling the tool.
4. If approved (or risk < `L3`), executes the tool through `ToolRegistry` and records `EXECUTED_OK`.
