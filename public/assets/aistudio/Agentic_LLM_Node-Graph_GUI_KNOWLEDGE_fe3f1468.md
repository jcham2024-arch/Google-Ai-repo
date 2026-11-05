# knowledge.md — Agentic LLM Node-Graph GUI

## 1. Purpose & Scope
A desktop-class GUI for authoring, executing, and governing multi-agent LLM workflows.
Core model: **one orchestrator agent drives a tree of specialized agents autonomously
until a task list completes**. The user interacts primarily with the orchestrator; the
graph is a live execution observability surface; sub-agents are drill-down artifacts.

## 2. Core Architecture
- **Orchestrator-first chat.** The chat tab is the command console for the orchestrator,
  not a universal per-node chat. Goal in → plan + task list out → autonomous execution.
- **Agent tree is dynamic.** The design graph shows *possible* wiring; the execution
  tree shows *actual* runtime delegations (branches created by the orchestrator).
- **Autonomy by default.** Execution runs unattended until the task list completes.
  Human input is exception-based (approval gates, escalations, kill switch).
- **Signal separation.** Orchestrator reasoning / execution state / sub-agent detail are
  three distinct layers with distinct UIs. Show high-signal by default; drill down on demand.

## 3. Tabs (Information Architecture)
| Tab | Role |
|---|---|
| **Chat** | Orchestrator console; collapsible delegation cards; drill-down to node chats |
| **Graph** | Authoring canvas + live execution tree + scratchpad inspector |
| **Settings** | Prompt/tool/agent libraries, registries, scoping, defaults |
| **Control Center** | Live metrics, cost, audit trail, test harness, version history, kill switch |

## 4. Agent Node = Self-Contained Artifact
Each node is a portable bundle of four dimensions; dragging from library → editable copy.
- **Prompts** — system prompt + agent prompt; each linkable to library or detachable copy.
- **Tools** — curated subset from central registry; per-tool governance.
- **Permissions** — execution modes (Plan / Default / Auto) × permission categories
  (read / write / execute / external) = auto | ask | block matrix.
- **Resources** — files, RAG collections, other agents (exposed as `ask_agent_<name>`),
  APIs/MCP servers. Explicit allow-lists; additive on top of baseline; kill switch.

Node card shows glanceable **capability summary** (e.g., `3 tools · read-only · 2 resources`).

## 5. Libraries (first-class, centralized)
- **Prompt library:** `{{Variable}}` placeholders, versioning (save = new immutable
  version; update in place optional), tags, `/shortcut` quick-insert in chat input,
  AI-assisted authoring, per-field canned pickers (role, rules, tone, boundaries).
- **Tool registry:** all connected tools, auth status, risk level (read/write/destructive/
  network), allow/deny/ask rules, admin monitoring.
- **Agent library:** saved agent bundles (prompts+tools+permissions+resources) dropped
  onto canvas as editable copies. Library edits never propagate to canvas copies.

## 6. Chat Console (orchestrator)
- Orchestrator messages show **plan, delegation decisions, synthesis** — not raw sub-agent tokens.
- Each delegation = **collapsible card** under the spawning turn (indexed by tool-call ID):
  role, status, one-line progress, "Open <agent> chat" drill-down.
- **Threads** within a chat for side work (Slack model: one durable chat per agent, threads
  for branching). Permission mode is thread state; new chats start at Default.
- Header shows agent identity + capability badge + breadcrumb back to node on canvas.
- Chat prompt shortcuts (`/`) surface prompt library popover.

## 7. Graph / Execution Tree
- **Design view:** static authored topology; Merge node (explicit multi-input with strategy:
  concat / LLM-summarize / last-wins), Fan-out node (parallel branches), typed ports
  (invalid connections grayed out + hover tooltip), searchable drag-drop palette.
- **Execution view:** live agent hierarchy — status badges (Idle/Running/Blocked/Done/Failed),
  progress, tool calls; DAG/tree + task-board (Kanban) + activity feed hybrid.
- **Scratchpad inspector:** shared structured state (single source of truth) visible as an
  artifact pane — what each agent read/wrote, without exposing raw inter-agent chatter.
- Global controls: pause / resume / cancel / **kill switch** (global + per-agent).

## 8. Context-Efficient Inter-Agent Communication (surfaced in UI)
- **Shared scratchpad pattern:** orchestrator holds state; each sub-agent gets a scoped task
  + fresh context; writes only to its designated section; no cross-section reads.
- **Pointer-based messages:** agents exchange lightweight references to shared state instead
  of verbose prose payloads (≈12.5k tokens → near-zero).
- Delegation cards show **what was passed** (scoped task) and **what was written** (scratchpad ref),
  not raw message bodies. Raw trace available only on drill-down.

## 9. Human-in-the-Loop & Autonomy
- **Watch mode (default):** task list updates live; no interrupts for routine work.
- **Intervention:** pause/redirect/inject mid-task; conversational control of root agent.
- **Approval gates:** interrupt/resume at checkpoints before sensitive tools or low-confidence
  steps. Inline chat cards (Allow / Allow-for-chat / Deny); "Allow-for-chat" is session-scoped.
- **Confidence bands** (High/Med/Low) modulate UI: High → default Accept; Low → mandatory edit.
- **Kill switch:** global + per-agent; cost-runaway auto-pauses and escalates with replay.

## 10. Observability, Debugging, Replay
- Per-node status + token-by-token streaming (graph as execution trace, not black box).
- **Aggregated vs Expanded** views for loops (`retrieve_docs (3/3)` ⇄ per-call).
- **Checkpoints + time-travel:** resume from any saved state; "what-if" re-runs.
- **Deterministic replay:** scrub frame-by-frame through inputs/outputs/internal state.
- **Session timeline / audit trail:** every tool call, model call, policy decision, approval, cost.

## 11. Cost & Budget
- Real-time spend by agent / session / model / workflow; filterable views + charts.
- Budgets per project/agent/workflow with color-coded warnings and hard stops.

## 12. Version Control & Collaboration
- **Semantic graph diffing/merging** (nodes + connections, not text diff).
- Visual history + branching for experimentation without touching main workflow.

## 13. Testing & Evaluation Harness
- Scenario library + test cases runnable against single agent or full graph; pass/fail stats.
- **Baseline + regression diffing** across runs (changed prompt/model/graph → catch regressions).

## 14. Workspace & Navigation
- **Novice vs Expert mode** toggle (hide advanced params + stronger defaults ⇄ full control).
- **Progressive disclosure** everywhere: card summary → full editor → runtime detail.
- Cross-surface linking: graph ⇄ chat ⇄ registry ⇄ library without losing context.
- Decision journaling ("Why I picked this") on critical human choices → searchable log.

## 15. End-to-End Execution Experience
1. User states goal in orchestrator chat → plan + task list.
2. Graph tab activates execution tree; agents appear as spawned with live status.
3. Watch mode: task board + tree update; orchestrator chat shows delegation summaries.
4. Exceptions only: approval gate on risk, auto-pause on cost runaway, manual pause/redirect/kill.
5. Drill-down optional: delegation card → sub-agent chat + scratchpad section.
6. Completion: task list resolves → synthesized result in orchestrator chat; full trace preserved.

## 16. Guiding Design Principles
1. **Orchestrator is the interface; agents are the implementation.**
2. **Autonomy by default, intervention by exception.**
3. **Signal separation:** high-level by default, detail on drill-down.
4. **Every artifact is inspectable and portable** (agent nodes, prompts, tools).
5. **Context efficiency is a first-class UX concern** (scratchpad + pointers, not chatter).
6. **Governance is visible, not buried** (permissions matrix, kill switch, cost, audit).
7. **Progressive disclosure** across all four tabs.
8. **Trust is calibrated** (confidence bands, approval gates, replay before destructive action).