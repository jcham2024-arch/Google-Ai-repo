# Networking Architecture & Resilience Specification

## Overview
Apex routes all outbound network operations through the pure Java 11 `HttpTransportPort` interface defined in `:core-engine`. In `:app`, the `OkHttpTransport` adapter implements this interface using OkHttp, providing industrial-grade connection pooling, configurable socket timeouts, exponential backoff retries, and Server-Sent Events (SSE) streaming.

## Architectural Guardrails
- **Guardrail C6**: Every network call MUST route through `HttpTransportPort`. Direct usage of `HttpURLConnection` or raw sockets in application feature code is strictly forbidden and enforced via CI static analysis.

## Transport Interface (`HttpTransportPort`)
```java
public interface HttpTransportPort {
    HttpResponse execute(HttpRequest request);
    void stream(HttpRequest request, ChunkConsumer consumer);
}
```

## Retry Policy & Matrix

All requests execute under an explicit `RetryPolicy` configured with exponential backoff and jitter:
$$\text{delay} = \min(\text{baseDelay} \times 2^{\text{attempt}-1} \pm \text{jitter}, \text{maxDelay})$$

### Retry Decision Matrix
| Status / Condition | Retry? | Strategy / Behavior |
| :--- | :--- | :--- |
| `429 Too Many Requests` | **Yes** | Respect `Retry-After` header if present; otherwise apply backoff delay. |
| `500`, `502`, `503`, `504` | **Yes** | Server-side transient error; apply exponential backoff. |
| `408 Request Timeout` | **Yes** | Request timed out on server; retry. |
| `400`, `401`, `403`, `404`, `422` | **No** | Client error; non-retryable. Return immediate failure. |
| `IOException` / Socket Timeout | **Yes** | Transport I/O failure; apply exponential backoff. |
| Interrupted / Cancelled | **No** | Propagate thread interrupt immediately. |

## Server-Sent Events (SSE) Streaming
For streaming endpoints (e.g. `/chat/completions` with `"stream": true`), `OkHttpTransport` sets the header `Accept: text/event-stream`. Lines starting with `data:` are parsed and dispatched to `ChunkConsumer` as `HttpChunk.data(...)`. When the `[DONE]` sentinel is encountered, `HttpChunk.done()` is emitted and the stream terminates cleanly.
