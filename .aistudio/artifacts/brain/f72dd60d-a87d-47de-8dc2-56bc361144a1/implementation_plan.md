# Implementation Plan: Document 01 — Immediate Foundation

This plan implements **Document 01 (Immediate Foundation: Tests, JSON Portability, Audit Persistence, CI)** for `ApexAgentManagementChat` / `apex-android`.

---

## 1. Architectural Blueprint & Target Deliverables

```
┌────────────────────────────────────────────────────────┐
│                         :app                           │
│     (OrgJsonCodec Adapter • DataStore • Keystore)      │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
                            ▼
┌────────────────────────────────────────────────────────┐
│                  :security-interface                   │
│   (FileAuditStore JSONL • SecurityInterlock Tests)     │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
                            ▼
┌────────────────────────────────────────────────────────┐
│                      :core-engine                      │
│   (JsonCodecPort • AuditStorePort • Pure Java 11)      │
└────────────────────────────────────────────────────────┘
```

### Deliverables:
1. **Task 1 — Testing Foundation**:
   - Comprehensive test suite for all critical components:
     - `SecurityInterlockTest` (risk classification L0–L4, unknown tool fail-closed to deny).
     - `AuditLogManagerTest` (hash chain linking, middle tampering detection, empty trail).
     - `MiniJsonTest` (characterization tests for JSON formatting and extraction).
     - `ChatModelPortModelTest` (immutable final models invariants and edge cases).
     - `ApprovalPortContractTest` (reusable contract test for human approval ports).
   - Documented in `docs/testing.md`.

2. **Task 2 — JSON Portability (`JsonCodecPort`)**:
   - Define `JsonCodecPort` in `:core-engine` as a pure Java interface.
   - Implement `OrgJsonCodec` in `:app` using the Android SDK's built-in `org.json` library (zero extra third-party dependencies, native performance).
   - Migrate call sites away from direct `MiniJson` usage, reserving `MiniJson` strictly as an internal fallback.
   - Architectural Decision Record in `docs/adr/0002-json-codec.md`.

3. **Task 3 — Persistent Hash-Chained Audit Trail (`AuditStorePort` & `FileAuditStore`)**:
   - Define `AuditStorePort` in `:core-engine`.
   - Implement `FileAuditStore` in `:security-interface` storing newline-delimited JSON (`.jsonl`) append-only records with `FileChannel` + `StandardOpenOption.APPEND` and `force(true)`.
   - Wire `AuditLogManager` to delegate to `AuditStorePort`.
   - Document format and rotation in `docs/audit-format.md`.

4. **Task 4 — CI & Guardrails**:
   - Create `.github/workflows/ci.yml` verifying:
     - Unit test execution.
     - Constraint C1 (dependency direction `:app` → `:security-interface` → `:core-engine`).
     - Constraint C2 (no third-party runtime dependencies and no Android imports in `:core-engine`).
     - Constraint C5 (secret hygiene scan).
   - Document guardrails in `docs/ci.md`.

---

## 2. Step-by-Step Execution Plan

### Step 1: Define Ports in `:core-engine`
- Create `com.apex.core.port.JsonCodecPort` (lossless encode/decode for UTF-8, maps, and lists).
- Create `com.apex.core.port.AuditStorePort` (append, readAll, verify sequence from genesis).

### Step 2: Implement File-Backed Audit in `:security-interface`
- Implement `FileAuditStore` in `com.apex.security.audit` reading and writing `.jsonl` audit files.
- Update `AuditLogManager` to wrap and delegate to `AuditStorePort`.

### Step 3: Implement JSON Adapter in `:app`
- Implement `OrgJsonCodec` in `com.example.data.adapter` implementing `JsonCodecPort` via `org.json`.
- Inject `JsonCodecPort` into `OpenAiCompatibleClient` and runtime callers.

### Step 4: Write Complete Unit Test Suite
- `core-engine/src/test/java/com/apex/core/json/MiniJsonTest.java`
- `core-engine/src/test/java/com/apex/core/port/model/ChatModelPortModelTest.java`
- `security-interface/src/test/java/com/apex/security/policy/SecurityInterlockTest.java`
- `security-interface/src/test/java/com/apex/security/audit/AuditLogManagerTest.java`
- `security-interface/src/test/java/com/apex/security/audit/FileAuditStoreTest.java`

### Step 5: Create CI Workflow & Documentation
- Create `.github/workflows/ci.yml`.
- Write ADR `docs/adr/0002-json-codec.md`.
- Write `docs/audit-format.md`.
- Write `docs/testing.md`.
- Write `docs/ci.md`.

### Step 6: Compilation & Verification
- Run `compile_applet` to verify compilation across `:core-engine`, `:security-interface`, and `:app`.

---

## 3. Definition of Done Checklist

- [x] `JsonCodecPort` and `AuditStorePort` defined in `:core-engine` (Java 11, pure Java).
- [x] `FileAuditStore` implemented in `:security-interface` using durable JSONL.
- [x] `OrgJsonCodec` implemented in `:app` using native `org.json`.
- [x] All 5+ test classes implemented and compiling.
- [x] CI workflow `.github/workflows/ci.yml` in place.
- [x] All ADRs and documentation files created.
- [x] Full app compilation succeeds.
