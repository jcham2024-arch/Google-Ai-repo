# Versioning and Releases

## Version sources

| Field | Location | Current value |
| :--- | :--- | :--- |
| `versionCode` | `app/build.gradle.kts` | 5 |
| `versionName` | `app/build.gradle.kts` | `5.0` |
| `applicationId` | `app/build.gradle.kts` | `com.aistudio.apexagentic.masterchat` |
| Room schema version | `data/local/AppDatabase.kt` | 9 |
| Gradle distribution | `gradle/wrapper/gradle-wrapper.properties` | 9.3.1 (wrapper script and JAR committed) |
| Changelog | `CHANGELOG.md` | Keep-a-Changelog headings with bracketed versions |

Versions are edited in source. The release workflow reads the git tag and cross-checks it against the
changelog; it does not derive or rewrite `versionCode`/`versionName`.

## Release workflow

`.github/workflows/release.yml` runs on tag pushes:

1. Asserts `CHANGELOG.md` contains a section for the tag (`grep "\[$tag\]"`) **and** that the tag
equals `versionName` in `app/build.gradle.kts`, failing the release otherwise.
2. Requires the `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD` and `RELEASE_KEY_PASSWORD`
   repository secrets, and fails with a named list when one is missing.
3. Decodes the upload keystore into `$RUNNER_TEMP` and exports `KEYSTORE_PATH`, `STORE_PASSWORD` and
   `KEY_PASSWORD`, which the AI Studio `release` signing config in `app/build.gradle.kts` reads.
4. Runs `./gradlew :app:assembleRelease :app:bundleRelease test --no-daemon`, so Gradle signs both
   the release APK and the release AAB.
5. Verifies both signatures (`apksigner verify --print-certs` on the APK, `jarsigner -verify` on the
   AAB) and fails if either artifact is missing.
6. Writes `app/build/outputs/SHA256SUMS.txt`, signs the APK and AAB blobs with cosign
   (`sign-blob --bundle`), and attaches artifacts, bundles and checksums to the GitHub release.

Remaining gaps, verified in the workflow file:

- `versionCode` is still a hand-edited constant in `app/build.gradle.kts`; a release tag that reuses
  a shipped version code is rejected by Play.
- A single universal APK is published; there is no ABI or API-level split.
- The `release` signing config hardcodes `keyAlias = "upload"`, so the upload keystore must use that
  alias or the build fails at the signing step.

## Signing

`app/build.gradle.kts` declares two signing configs:

| Config | Source |
| :--- | :--- |
| `release` | `storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")` with store/key password env vars; the keystore file itself is not in the repository |
| `debugConfig` | `${rootDir}/debug.keystore` |

The build script carries a comment stating the signing config assignments must not be modified for
AI Studio builds.

## Changelog policy

- Add changes under `[Unreleased]` while working; see [CONTRIBUTING.md](../CONTRIBUTING.md).
- Move entries into a dated, versioned heading when tagging.
- The release job refuses to publish if the tag has no changelog section.

## Recommended next steps

1. Derive `versionName` (and `versionCode`) from the tag at build time instead of editing two files
   by hand.
2. Add a Room migration policy before shipping schema changes, because the current
   `fallbackToDestructiveMigration()` discards user data.
