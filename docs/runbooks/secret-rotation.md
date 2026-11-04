# Runbook: API Key and Secret Rotation

## Symptoms

- Provider requests return HTTP 401 or 403 for a provider that used to work.
- A key is suspected leaked (for example it appears in a shared log or was committed by accident).

## Impact

Generation and embeddings fail for the affected provider. Nothing else is affected: keys are stored
per provider and tool execution does not depend on the model provider.

## Procedure

1. Revoke the old key at the provider console first. Rotation in the app does not invalidate the old
   credential upstream.
2. Open the provider configuration screen and select the affected provider card.
3. Enter the new key. On save, `ProviderConfigurationManager` encrypts it through
   `AndroidKeyStoreManager.encrypt`, which uses an Android Keystore AES-256/GCM key with a random
   12-byte IV prefix and Base64 encoding, then persists the ciphertext in DataStore.
4. Run the built-in connection test (`ProviderConfigurationManager.testConnection`): `GET /models`
   first, then a minimal `POST /chat/completions`.
5. Confirm a real turn succeeds and that the audit record for the next tool call appears.

## Verifying containment after an accidental commit

1. Check the repository history for the value; `.env` is ignored, but a hardcoded key in a source file
   would be caught only by the non-blocking gitleaks step (C5). Check the CI log for the commit range.
2. If a key was committed, rotate it regardless of whether the branch was merged, and consider the
   value public from the moment it was pushed.
3. `debug.keystore` is committed by design for debug builds; it must never be reused for release
   signing. Release signing reads `KEYSTORE_PATH` from the environment, falling back to
   `my-upload-key.jks`, which is not in the repository.

## Notes and caveats

- Keystore entries are not deleted when a key is replaced, so old key material can remain in the
  Keystore until the entry is explicitly removed.
- There is no scheduled rotation and no expiry tracking; rotation is event-driven.
- Rotating a provider key does not rotate the audit chain or any signing material.
