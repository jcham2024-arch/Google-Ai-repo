# Apex Agentic Platform Expansion: Swarm Autonomy, Workspace Governance & Background Sync

Implementation plan for integrating sub-agent swarm delegation, full line-by-line diff authorization, and automatic background queue synchronization into Apex Agentic Master Chat.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during Phase 1 user clarification and govern this implementation plan:

- **Confirmed Decision 1: Agent Swarm Task Handoffs**
  *Choice*: Automatic sub-agent delegation with a live visual chain of thought indicator in the Chat UI.
  *Impact*: `AgentOrchestrator` automatically invokes specialized agents (e.g., Coder, Researcher, Architect) via `DelegateTaskToolWorker` and streams delegation status events to the UI timeline.

- **Confirmed Decision 2: Workspace Authorization & Diff Inspection**
  *Choice*: Full line-by-line diff preview modal for human authorization prior to file mutations.
  *Impact*: All `write_file` and `apply_edit` tool calls trigger a Material 3 dialog rendering unified line diffs (additions in green/tertiary, deletions in red/error) before execution.

- **Confirmed Decision 3: Offline Queue Recovery & Replay**
  *Choice*: Automatic background processing with a persistent UI progress banner upon network reconnection.
  *Impact*: Android `WorkManager` with `NetworkType.CONNECTED` constraint automatically processes queued `PendingActionEntity` records and notifies the user with a sync banner in Settings and Chat.

---

## 1. Overview & Core Concept

**What It Does**:
Apex Agentic Master Chat evolves into a fully autonomous multi-agent orchestration platform on Android. It allows users to prompt a primary Master Agent that can intelligently delegate complex sub-tasks to specialized Agent Swarm workers, inspect and self-modify workspace source files with strict human-in-the-loop diff approvals, and seamlessly queue messages or actions offline for automatic background execution when back online.

**Target Audience / Persona**:
Mobile developers, software engineers, and power users who need an offline-first AI agent environment that respects security boundaries, maintains tamper-evident audit logs, and supports background task completion.

**Key Value**:
- *True Multi-Agent Autonomy*: Agents delegate to specialized sub-agents with visual trace cards.
- *Zero-Trust Workspace Governance*: Every file change is inspectable via line-by-line diffs before touching disk.
- *Resilient Offline Operations*: WorkManager ensures tasks are never lost during connectivity drops.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Agent Swarm Handoff & Chain of Thought**:
   - User inputs a multi-step prompt in `ChatScreen.kt` (e.g., "Analyze the project structure and create a new repository module").
   - The Master Agent emits a `delegate_task` tool call.
   - The Chat UI renders a **Delegation Chain Banner** showing `Master Agent ➔ Coder Agent (EXECUTING)`.
   - The delegated agent executes the task and returns the result back to the Master Agent, updating the status badge to `COMPLETED`.

2. **Workspace Line-by-Line Diff Approval**:
   - An agent proposes a file mutation (`write_file` or self-edit).
   - The `SecurityInterlock` flags the action as `L3` (High Risk) and opens the `AlertDialog`.
   - The dialog displays a scrollable **Diff Inspector** featuring line numbers, `+` green additions, `-` red deletions, and context lines.
   - User reviews the exact diff and clicks **Approve** (executes write) or **Deny** (cancels with audit record).

3. **Offline Queue Banner & Settings Health Summary**:
   - When offline, prompts are saved to `PendingActionEntity` via `OfflineQueueDao`.
   - A subtle top banner in `ChatScreen` and `SettingsScreen` alerts: `📶 Offline Mode: 2 tasks queued`.
   - When connection returns, `WorkManager` automatically triggers `OfflineSyncWorker`, processes pending tasks, and updates the Settings summary view.

### Visual Identity & Theme

- **Aesthetic Direction**: High-contrast, industrial precision dark/light theme utilizing Material 3 tonal elevation and semantic color tokens (`surfaceContainer`, `tertiaryContainer`, `errorContainer`).
- **Typography**: Clean monospace font for code blocks, diffs, and tool arguments (`FontFamily.Monospace`) paired with M3 `titleMedium` display headers.
- **Components**:
  - *Swarm Trace Card*: Rounded elevation card with animated state icons (`HourglassEmpty`, `CheckCircle`, `ArrowForward`).
  - *Diff Line*: Distinct background tints (`tertiaryContainer` @ 30% alpha for additions, `errorContainer` @ 30% alpha for deletions).
  - *Health Dashboard Card*: Settings summary card showing Connection Health (Green/Orange pill), Pending Sync Queue count, and Active Swarm count.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Jetpack WorkManager for Background Queueing**
  - *Chosen Approach*: `WorkManager` `OneTimeWorkRequestBuilder` coupled with Room `OfflineQueueDao`.
  - *Why*: Ensures background execution persists across process death, device reboots, and network state changes.
  - *Alternatives Considered*: In-memory coroutine queue (lost on app close) or direct broadcast receivers (deprecated on Android 8+).

- **Decision 2: Universal Security Interlock with Diff Generation**
  - *Chosen Approach*: In-memory diff computation (`DiffUtil.generateDiff`) injected directly into the `ApprovalRequest` payload before reaching the UI.
  - *Why*: Keeps security evaluation pure and centralized in `:security-interface` while giving the Compose UI pre-computed diff structures.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  (ChatScreen • SettingsScreen • AgentListScreen • WorkspaceScreen)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlow / Actions
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            Domain Layer                                │
│   ┌───────────────────────┐             ┌──────────────────────────┐   │
│   │   AgentOrchestrator   │────────────►│     ProviderRegistry     │   │
│   └───────────┬───────────┘             └──────────────────────────┘   │
│               │ Tool Call / Handoff                                    │
│               ▼                                                        │
│   ┌───────────────────────┐             ┌──────────────────────────┐   │
│   │ SecurityInterlock     │────────────►│  FileGovernanceEngine    │   │
│   │ (L0..L4 Risk Gate)    │             │  (Workspace Sandbox)     │   │
│   └───────────┬───────────┘             └──────────────────────────┘   │
└───────────────┼────────────────────────────────────────────────────────┘
                │ Requires Approval / Audit
                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Data & Storage Layer                            │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │     AppDatabase (Room)  │  │   WorkManager / OfflineSyncWorker   │  │
│  │ (AgentCard, Pending,    │  │ (NetworkType.CONNECTED Constraint)  │  │
│  │  Messages, Workspace)   │  └─────────────────────────────────────┘  │
│  └─────────────────────────┘                                           │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Entities & Schema Updates

1. **`AgentCard` (Room Entity)**:
   - `id: String` (PK)
   - `name: String`
   - `description: String`
   - `systemPrompt: String`
   - `preferredProvider: String?`
   - `preferredModel: String?`
   - `isNarrator: Boolean`

2. **`PendingActionEntity` (Room Entity)**:
   - `actionId: String` (PK)
   - `actionType: String` (`CHAT_MESSAGE`, `TOOL_EXECUTION`)
   - `payloadJson: String`
   - `status: String` (`PENDING`, `SYNCING`, `COMPLETED`, `FAILED`)
   - `timestamp: Long`

3. **`WorkspaceFileVersion` (Room Entity)**:
   - `id: Long` (Auto-PK)
   - `filePath: String`
   - `content: String`
   - `sizeBytes: Long`
   - `version: Int`
   - `timestamp: Long`
   - `changeType: String` (`CREATE`, `MODIFY`, `RESTORE`)

---

## 5. Implementation Roadmap & Milestones

- **Milestone 1: Security & Diff Inspector Integration**
  - Verify line-by-line diff generation in `DiffUtil.kt`.
  - Wire `AndroidApprovalPort` to compute diffs when `write_file` is requested.
  - Verify `ChatScreen` AlertDialog displays unified diff preview.

- **Milestone 2: Agent Swarm & Delegation Trace**
  - Ensure `DelegateTaskToolWorker` registers in `ToolRegistry`.
  - Connect delegation callbacks to `ChatViewModel.startDelegation()`.
  - Test multi-agent delegation flow in `AgentListScreen` and `ChatScreen`.

- **Milestone 3: WorkManager Offline Queue & Settings Summary Dashboard**
  - Verify `AgentTaskWorker` enqueues and processes tasks with `WorkManager`.
  - Update `SettingsScreen.kt` to show Connection Health, Queue Count, and Active Agent Swarm count.
  - Run full build verification (`compile_applet`).
