# Runbook: LLM Provider Outage

## Symptoms
Model generation calls fail with persistent 5xx HTTP errors or rate-limit timeouts. `ratelimit.throttled` counter spikes.

## Impact
Chat generation requests time out or fail.

## Diagnosis
1. Run live endpoint connectivity check via `ProviderConfigurationManager.testConnection()`.
2. Inspect provider status pages (OpenAI, DeepSeek, Anthropic, Groq).

## Mitigation
1. Open Provider Settings screen in app.
2. Select an alternative active provider preset (e.g. switch from primary to secondary provider).
3. System automatically routes new requests through the newly active provider.
