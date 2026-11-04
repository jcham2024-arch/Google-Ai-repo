# Runbook: LLM Provider Outage

## Symptoms

- Chat turns fail with HTTP 4xx/5xx from the provider.
- Transport logs show repeated retries with growing backoff, or `Retry-After` delays being honoured.
- Users report slow or missing responses while the app stays responsive.

## Impact

Model generation fails or is delayed. Tool execution, the audit chain, and the local UI continue to
work, so failure is contained to the turn.

## Diagnosis

1. Use the in-app provider test: `ProviderConfigurationManager.testConnection(baseUrl, apiKey, model)`
   tries `GET {baseUrl}/models`, then falls back to a minimal `POST {baseUrl}/chat/completions`. A 401
   indicates a credential problem, not an outage.
2. Read the transport behaviour: `429`, `408` and `5xx` are retried up to `RetryPolicy.maxAttempts`
   with `Retry-After` honoured when present; `IOException` (timeouts, DNS) is retried too.
3. Check the provider's own status page before assuming a client bug.
4. Confirm which provider is active: `ProviderRegistry.activeProvider` (persisted in DataStore).

## Mitigation

1. Open Settings → provider configuration.
2. Select another preset (for example switch from a primary cloud provider to a secondary one) and
   save. `ProviderRegistry.selectActiveProvider` persists the choice; subsequent turns use it.
3. Re-run the connection test to confirm the new provider answers.
4. If no provider is reachable, keep working offline: queued messages are stored in
   `pending_actions` and replayed by `OfflineSyncManager` when connectivity returns.

## What does not happen automatically

There is no automatic provider failover, no circuit breaker, and no health-based routing. The switch
is manual. Rate limiting is implemented (`TokenBucketRateLimiter`) but not applied to provider
requests, so the client cannot shield a provider from retry storms.

## Follow-up

- If retries amplified the outage, raise `RetryPolicy` delays before increasing connection pool size.
- Capture the failing request id, status code and `Retry-After` value for the incident record.
