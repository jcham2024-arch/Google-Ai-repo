# Tool Adapter Security Checklist

Every `ToolWorker` in `app/src/main/java/com/example/domain/tools/` runs behind `SecurityInterlock`.
Risk classification comes from `ToolRiskRegistry`, not from the worker.

## Current risk map (verified in `ToolRiskRegistry.java`)

| Tool | Risk | Approval required |
| :--- | :--- | :--- |
| `read_file` | L2 | no |
| `list_files` | L2 | no |
| `http_get` | L2 | no |
| `write_file` | L3 | yes |
| `write_file_external` | L3 | yes |
| `http_post` | L3 | yes |
| `run_command` | L4 | yes |
| any unregistered name | L3 (fail-closed default) | yes |

An earlier version of this document listed `write_file` as L1 and `http_get` as L2 in a way that
implied no approval; the table above is the behaviour that is actually implemented and tested
(`SecurityInterlockTest.testUnknownToolFailsClosedToL3`).

## Checklist for a new or changed worker

1. Classify it: add an explicit `registerRisk(toolName, level)` entry or a seed line in
   `ToolRiskRegistry`. Never rely on the L3 default for a tool that is genuinely read-only, and never
   lower a level to avoid the approval prompt.
2. Validate every argument before use; throw `IllegalArgumentException` for a missing or malformed
   value. There is no schema validation layer yet.
3. Validate paths through `WorkspacePathGuard.resolve` (rejects absolute paths, `..`, and canonical
   escapes) instead of joining raw strings to a base directory. `FileGovernanceEngine` and both read
   workers share this object, and every worker receives the same `filesDir/workspace` root, so the
   read surface cannot exceed the write surface.
4. Never execute untrusted text as code, shell, or bytecode. `run_command` fails closed with
   `UnsupportedOperationException` and executes nothing; it must never return a success string that
   implies a side effect.
5. Return informative messages without stack traces or absolute device paths.
6. Keep output bounded; large results are truncated by `AgentOrchestrator.truncateToolResult` before
   returning to the model.
7. Do not call `TelemetryPort` directly from a worker — the interceptor already emits `tool.invoke`,
   `tool.invocations`, `tool.duration_ms` and `security.denials`. If you must, wrap through
   `SafeTelemetryDecorator`.
8. Add a unit test that asserts both the result and the interlock outcome (approval requested, or
   denial recorded in the audit chain).

## Actor-tool capabilities worth reviewing before release

| Tool | Concern |
| :--- | :--- |
| `create_tool` | Registers new tool definitions at runtime; a registered tool inherits default L3 gating unless explicitly classified |
| `delegate_task` | Spawns sub-agent work through the interlock; budget accounting is per interlock instance, not per delegation tree |
| `http_post` | Sends model-chosen payloads to model-chosen URLs; an allow-list of hosts does not exist today |
| `search_memory` / `save_memory` | Memory rows feed future prompts, so untrusted content can persist into later turns |
| `write_file` | Validated by `FileGovernanceEngine`, capped at 10 MiB per file, versioned on every write |
| `run_command` | Refuses every call today; before implementing, decide the allowed command set, timeout, and output cap, and keep the level at L4 |
