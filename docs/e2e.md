# End-to-End Integration Testing Guide

## Overview
Apex uses Robolectric and OkHttp `MockWebServer` to run fast, deterministic JVM-based end-to-end integration tests without requiring an Android emulator.

## Test Location
All E2E integration test cases live in:
`app/src/test/java/com/example/e2e/AgentPipelineE2ETest.kt`

## LLM Response Fixtures
Mock LLM responses are frozen as JSON or SSE files in:
`app/src/test/resources/llm-fixtures/`

### Fixture Registry
- `simple-reply.json`: Single-shot text completion response.
- `tool-call-single.json`: Single function tool call response (`read_file`).
- `tool-call-nested-args.json`: Tool call response with nested JSON argument maps (`run_command`).
- `streamed-reply.sse`: Server-Sent Events stream emitting incremental token deltas and `[DONE]`.
- `rate-limited-then-ok.json`: Mock response following HTTP 429 rate limit retry recovery.

## Adding a New E2E Test Scenario
1. Add new payload fixture to `app/src/test/resources/llm-fixtures/my-scenario.json`.
2. Enqueue the fixture on `mockWebServer`:
   ```kotlin
   mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(readFixture("my-scenario.json")))
   ```
3. Execute request via `OpenAiCompatibleClient` or `ToolInvocationInterceptor`.
4. Assert response success, telemetry signals, and audit chain integrity via `fileAuditStore.verify()`.
