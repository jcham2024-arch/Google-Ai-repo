# Testing Strategy and Test Suite Index

## Philosophy
Apex enforces a test-driven architecture with zero flakiness. All core business logic and security policies are tested using local JVM test runners without requiring an Android emulator or UI mocks.

## Test Matrix by Module

| Module | Test Class | Focus Area |
| :--- | :--- | :--- |
| `:core-engine` | `com.apex.core.json.MiniJsonTest` | Characterization tests for JSON formatting, escaping, nested maps/lists, null-handling, and field extraction. |
| `:core-engine` | `com.apex.core.port.model.ChatModelPortModelTest` | Value object contracts, null-safety defaults, immutability, `equals()`, and `hashCode()`. |
| `:core-engine` | `com.apex.core.port.ApprovalPortContractTest` | Approval lifecycle contract test verifying approved/denied states. |
| `:security-interface` | `com.apex.security.policy.SecurityInterlockTest` | Risk categorization (L0–L4), fail-closed unknown tool handling (defaulting to L3), approval gate rejection, and error recovery. |
| `:security-interface` | `com.apex.security.audit.AuditLogManagerTest` | Hash-chain integrity, SHA-256 calculation, and sequential linking against genesis block. |
| `:security-interface` | `com.apex.security.audit.FileAuditStoreTest` | Append-only `.jsonl` durability, state recovery across re-openings, `fsync` forcing, tampering detection, and log rotation. |

## Running Tests

To run all JVM tests across all modules:
```bash
gradle test
```

To run module-specific unit tests:
```bash
# Pure Java core engine tests
gradle :core-engine:test

# Security interface Android unit tests
gradle :security-interface:testDebugUnitTest

# App unit tests
gradle :app:testDebugUnitTest
```

## Writing New Tests
1. **Core Engine**: Pure JUnit 4 tests in `core-engine/src/test/java/`. No Android SDK dependencies.
2. **Security Interface**: Unit tests in `security-interface/src/test/java/`. Use temporary directories (`System.getProperty("java.io.tmpdir")`) for disk testing.
3. **No Mocks**: Prefer real lightweight implementations or stub ports (e.g. `ApprovalPort` lambda) over heavyweight dynamic mocking frameworks.
