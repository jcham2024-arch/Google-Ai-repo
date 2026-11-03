# Tool Adapter Security Checklist & Schema Requirements

## Overview
Every tool integrated into Apex must declare a strict schema and execute under `SecurityInterlock` governance.

## Mandatory Security Checklist for Tool Workers
When adding or updating a `ToolWorker` implementation:

- [ ] **Declare Tool Schema**: Define required parameters using `ToolSchema.create(toolName, "param1", "param2")`.
- [ ] **Input Sanitization**: Validate file paths, URL inputs, and command parameters before execution.
- [ ] **No Dynamic Code Execution**: Do NOT execute raw untrusted string scripts or bytecode via `eval` or dynamic loading.
- [ ] **Assign Risk Level**: Register appropriate `RiskLevel` in `ToolRiskRegistry`:
  - `L0`: Read-only local inspection (`read_file`, `list_files`).
  - `L1`: Local sandboxed state modification (`write_file`).
  - `L2`: External network GET / read operations (`http_get`).
  - `L3`: External network POST or mutating state (`http_post`).
  - `L4`: Command execution or elevated privilege (`run_command`).
- [ ] **Error Handling**: Wrap tool execution logic in safe try-catch blocks and return informative error strings without leaking internal stack traces.
