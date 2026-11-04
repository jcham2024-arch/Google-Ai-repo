# Scalability and Limits

All values below are the constants that exist in code today. None of them is configurable from the UI.

## Concurrency and I/O

| Limit | Value | Source |
| :--- | :--- | :--- |
| HTTP idle connections | 5 | `OkHttpTransport`: `ConnectionPool(5, 5, TimeUnit.MINUTES)` |
| Connect timeout | 10 s | `OkHttpTransport` |
| Read / write timeout | 30 s each | `OkHttpTransport` |
| Max transport attempts | `RetryPolicy.maxAttempts` | `RetryPolicy` in `:core-engine` |
| Rate limiter capacity | 60 tokens | `TokenBucketRateLimiter` default |
| Rate limiter refill | 10 tokens/second | `TokenBucketRateLimiter` default |
| Tool concurrency | No explicit cap; the interlock is called per tool call | `SecurityInterlock` |

## Execution budgets

| Budget | Value | Source |
| :--- | :--- | :--- |
| Tool iterations per conversation | 10, `DEFAULT_MAX_ITERATIONS` | `SecurityInterlock` |
| Tool result truncation | applied by `AgentOrchestrator.truncateToolResult` before results re-enter the prompt | `AgentOrchestrator` |
| Offline queue | unbounded rows in `pending_actions`; replay drains in order | `OfflineQueueDao`, `OfflineSyncManager` |

Budget exhaustion is fail-closed: the call is denied, `DENIED_BUDGET_EXHAUSTED` is appended to the
audit chain, and `BudgetExhaustedException` is thrown.

## Storage

| Area | Behaviour |
| :--- | :--- |
| Room database | `AppDatabase` (schema version 9, `exportSchema = false`) holds conversations, messages, agents, workspace files and versions, memory items, lorebooks, provider configs and pending actions; it is built with `fallbackToDestructiveMigration()`, so any schema change wipes existing data — there are no migrations |
| Workspace sandbox | `FileGovernanceEngine` roots the sandbox at `filesDir/workspace` and rejects files above `MAX_FILE_SIZE_BYTES` (10 MiB) |
| Workspace versions | Each file write creates a `workspace_file_versions` row; nothing prunes old versions |
| DataStore | Provider preferences and the storage adapter key-value set |
| Keystore | One AES-256/GCM key entry per securable value; encryption is per-value, not per-file |
| Audit log | JSONL segments; rotation is manual via `FileAuditStore.rotate()`, and production currently keeps records in memory only |

## Known scaling limits

1. Tool execution is serial within a turn; there is no worker pool for parallel tool calls.
2. Rate limiting is implemented but not applied to provider requests, so provider-side throttling is
   the only backpressure.
3. The offline queue has no size limit, no backoff between replays, and no poison-message handling.
4. `FileGovernanceEngine` version history grows without bound; long sessions will grow the database.
5. Audit rotation is never triggered automatically in application code.
6. No pagination contract is documented for conversation or workspace lists; the Compose screens read
   whole flows.

## Capacity planning notes

- The engine module is allocation-light and stateless per request; scale limits are dominated by the
  transport (connection pool and timeouts) and the device storage.
- Because audit records store hashes rather than arguments, log growth is bounded by the number of
  tool calls, not by argument size.
- Raising `ConnectionPool` beyond the provider's per-key concurrency limit will produce 429s that the
  retry policy then amplifies; raise the retry delay before raising the pool.
