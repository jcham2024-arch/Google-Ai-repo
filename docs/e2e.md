# End-to-End Integration Testing

## Approach

`app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt` drives the request/response pipeline on the
JVM with OkHttp `MockWebServer`. No emulator is required; Robolectric supplies Android resources for
the rest of the unit-test source set.

## Harness

1. `MockWebServer` is started per test and its base URL is injected as the provider base URL.
2. A frozen response from `app/src/test/resources/llm-fixtures/` is enqueued.
3. The request goes through the engine client and transport adapters under test.
4. Assertions cover the parsed response, emitted telemetry counters/spans, and — where the interlock
   is in the path — the audit chain state via `FileAuditStore`/`AuditLogManager` verification.

## Fixtures

All five fixtures exist in `app/src/test/resources/llm-fixtures/`:

| Fixture | Scenario |
| :--- | :--- |
| `simple-reply.json` | Single-shot text completion |
| `tool-call-single.json` | One `read_file` tool call |
| `tool-call-nested-args.json` | Tool call whose arguments contain nested JSON (`run_command` shape) |
| `streamed-reply.sse` | SSE stream with incremental deltas and a terminating `[DONE]` |
| `rate-limited-then-ok.json` | HTTP 429 followed by a successful retry |

`load/LoadSmokeTest.kt` reuses the same harness for a throughput smoke check.

## Adding a scenario

1. Add the payload as a new file under `app/src/test/resources/llm-fixtures/`.
2. Enqueue it:

   ```kotlin
   mockWebServer.enqueue(
       MockResponse().setResponseCode(200).setBody(readFixture("my-scenario.json"))
   )
   ```

3. Drive the request through the pipeline under test (client, transport, or interlock).
4. Assert on the response, on telemetry signals, and on the audit chain when the interlock ran.

## Current limitations

- `AgentPipelineE2ETest.kt:103,159,174` references `OrgJsonCodec.INSTANCE`, which does not exist
  (`OrgJsonCodec` has no companion object), and `load/LoadSmokeTest.kt:73,139` calls `assertFalse`
  without importing it. Neither file compiles as written, so the E2E suite cannot currently run.
  See [status.md](status.md).
- No fixture exercises the streaming path end to end through the interlock; the SSE fixture is parsed
  by the transport only.
- The suite does not cover WorkManager-worker execution, Compose UI, or Firebase paths.
