# Runbook: Release Rollback

## Symptoms

A regression or security problem is found in a published release tag.

## Impact

Depends on the defect: from broken provider configuration to a tool-gating regression. Nothing in the
app is remotely updateable, so users must install a new build.

## Assessment first

1. Identify the last known-good tag: `git tag --sort=-creatordate | head`.
2. Determine whether the defect is in code, configuration, or data. A Room schema change is not
   rollback-safe: `AppDatabase` is built with `fallbackToDestructiveMigration()`, so downgrading or
   upgrading across a schema change wipes local data.
3. Decide between revert-and-tag and forward-fix. Never rewrite published tags.

## Rollback procedure

1. Revert the offending commit on `main`:
   `git revert <sha>` (prefer a revert commit over a history rewrite).
2. Run the local checks before tagging:
   `./gradlew test` and `./gradlew :app:assembleRelease` (see the wrapper caveat below).
3. Push the revert, then tag a patch release: `git tag vX.Y.Z+1 && git push origin vX.Y.Z+1`.
4. Confirm the release workflow publishes the new artifacts; it refuses to run without a matching
   `[X.Y.Z]` section in `CHANGELOG.md`, so add the section first.

## Known workflow limitations

- The Gradle wrapper is committed, but this workspace sits on shared storage, which is not
  exec-capable: `./gradlew` fails there with `bad interpreter: Permission denied`. Run `sh gradlew`
  on the phone; GitHub runners are unaffected.
- The release workflow requires the three signing secrets and fails the job when one is missing, so a
  tag pushed before those secrets exist produces no release rather than an unsigned one.
- Rollback does not restore local data on devices. The audit chain is now written to
  `filesDir/audit/audit_active.jsonl` in the app, but it lives on the device and is not shipped
  anywhere, so it still cannot be compared across releases.

## Verification after rollback

1. The new tag exists and its release assets are the expected APKs.
2. `CHANGELOG.md` documents the reverted change.
3. The previous defect no longer reproduces on a device install of the patch build.
