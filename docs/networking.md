# Networking

## Layers

| Layer | Type | Implementation |
| :--- | :--- | :--- |
| Engine client | `com.apex.core.client.OpenAiCompatibleClient` | Builds requests and parses responses using `HttpTransportPort` and `JsonCodecPort` only |
| Transport port | `com.apex.core.port.HttpTransportPort` | `execute(HttpRequest)` for buffered calls, `stream(HttpRequest, ChunkConsumer)` for streaming |
| App transport | `com.example.data.adapter.OkHttpTransport` | OkHttp implementation used by the app |
| Codec | `JsonCodecPort` | `OrgJsonCodec` in `:app`, `MiniJson` inside `:core-engine` |

Guardrail C6 forbids `HttpURLConnection` in `app/src/main/`, so provider calls go through
`HttpTransportPort`.

## OkHttp configuration (`OkHttpTransport.kt`)

| Setting | Value |
| :--- | :--- |
| Connect timeout | 10 s |
| Read timeout | 30 s |
| Write timeout | 30 s |
| Connection pool | `ConnectionPool(5, 5, TimeUnit.MINUTES)` — 5 idle connections, 5 idle minutes |
| Retry policy | `RetryPolicy()` default instance |

## Retry behaviour

- Retryable status codes: `429`, `408`, and any `5xx` (`OkHttpTransport.kt:138`).
- `Retry-After` response headers are parsed into a delay in milliseconds and take precedence over the
  policy delay.
- Backoff arithmetic, attempt limits and jitter live in `core-engine/src/main/java/com/apex/core/port/RetryPolicy.java`
  and are covered by `RetryPolicyTest`.

## Streaming

`stream(...)` reads the response body line by line:

- a line equal to `data: [DONE]` or `[DONE]` terminates the stream;
- a line starting with `data:` is trimmed of the prefix and forwarded as an `HttpChunk` data payload;
- transport failures are surfaced as error chunks rather than exceptions thrown into the caller.

## Provider endpoints

Requests are built from the active `ProviderSettings`: base URL, model id and max tokens. The seven
presets and their base URLs are listed in the [README](../README.md#supported-provider-presets).

`ProviderConfigurationManager.testConnection` first tries `GET {baseUrl}/models`, then falls back to
a minimal `POST {baseUrl}/chat/completions`, so both strict and permissive OpenAI-compatible servers
pass the test.

## Rate limiting

`TokenBucketRateLimiter` implements `RateLimiterPort` with a default capacity of 60 tokens and a
refill of 10 tokens/second, emitting `ratelimit.acquired`, `ratelimit.throttled` and
`ratelimit.timeout` counters. It is not yet referenced by the transport, so provider calls are
currently unthrottled.

## Timeouts and failure modes

| Failure | Current behaviour |
| :--- | :--- |
| Connect/read timeout, DNS failure, other `IOException` | Retried up to `retryPolicy.maxAttempts` with `delayForAttempt` backoff, then returned as a failure result |
| Interrupted thread | Not retried; the interrupt flag is restored and a failure result is returned |
| HTTP 429/408/5xx | Retried with backoff honouring `Retry-After` |
| Any failure on the streaming path | Not retried; the stream is emitted with an error chunk |
| Missing API key | Request is sent without an `Authorization` header; the provider returns 401 |
| Malformed JSON | Codec throws; the caller sees the failure and the turn aborts |
| SSE stream without `[DONE]` | Stream ends when the body closes |
