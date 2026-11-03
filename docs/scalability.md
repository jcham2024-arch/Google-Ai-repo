# Scalability & Concurrency Model

## Threading & Concurrency Architecture

Apex enforces explicit bounds on concurrency, thread dispatchers, and queue sizes across all modules.

### Thread Dispatcher Map
- **UI / Main Thread**: Renders Jetpack Compose UI. NEVER performs network, file I/O, or encryption calls.
- **`Dispatchers.IO`**: Executes network HTTP transport calls (`OkHttpTransport`), file I/O operations (`FileAuditStore`), and SQLite database operations.
- **`Dispatchers.Default`**: Bounded pool for tool worker execution and cryptographic hashing computations.

### Fixed Concurrency Bounds
- **Maximum In-Flight Model Requests**: Bounded to **10** concurrent requests via `OkHttpTransport` connection pool limits.
- **Maximum In-Flight Tool Worker Executions**: Bounded to **20** concurrent workers via `ToolRegistry` cached executor limits.
- **Maximum Queue Timeout**: 30,000 ms acquire timeout on `RateLimiterPort`. Exceeding timeout throws `RateLimitExceededException` or triggers `DEGRADED` state.
