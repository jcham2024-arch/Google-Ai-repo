# Runbook: API Key & Secret Rotation

## Symptoms
API requests return HTTP 401 Unauthorized due to revoked or rotated model provider API keys.

## Impact
LLM model generation fails for affected provider.

## Mitigation
1. Open Provider Settings screen in app.
2. Click "Change API Key" on target provider card.
3. Input new API key in modal dialog.
4. Key is automatically encrypted via AES-256 GCM in `AndroidKeyStoreManager` and saved to `DataStore`.
5. Click "Test Key" to verify endpoint authentication.
