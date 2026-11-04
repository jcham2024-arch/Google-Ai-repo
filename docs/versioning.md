# Versioning and Releases

## Version sources

| Field | Location | Current value |
| :--- | :--- | :--- |
| `versionCode` | `app/build.gradle.kts` | 5 |
| `versionName` | `app/build.gradle.kts` | `5.0` |
| `applicationId` | `app/build.gradle.kts` | `com.aistudio.apexagentic.masterchat` |
| Room schema version | `data/local/AppDatabase.kt` | 9 |
| Gradle distribution | `gradle/wrapper/gradle-wrapper.properties` | 9.3.1 (wrapper JAR not committed) |
| Changelog | `CHANGELOG.md` | Keep-a-Changelog headings with bracketed versions |

Versions are edited in source. The release workflow reads the git tag and cross-checks it against the
changelog; it does not derive or rewrite `versionCode`/`versionName`.

## Release workflow

`.github/workflows/release.yml` runs on tag pushes:

1. Asserts `CHANGELOG.md` contains a section for the tag (`grep "\[$tag\]"`), failing the release
   otherwise.
2. Runs `./gradlew assembleDebug test --no-daemon` with `RELEASE_VERSION` set from the tag.
3. Installs cosign (`sigstore/cosign-installer@v3`).
4. Writes `*.apk.sha256` checksum files next to each APK in `app/build/outputs/apk/debug`.
5. Publishes a GitHub release with the APKs and checksums attached.

Observed gaps, verified in the workflow file:

- The build publishes **debug** APKs, not release builds, even though the tag is a release tag.
- cosign is installed but never invoked, so nothing is actually signed beyond the Gradle signing
  config.
- The step runs `chmod +x ./gradlew || true` and then calls `./gradlew`; the wrapper does not exist in
  the repository. The release job as written fails at that step (there is no bare `gradle` fallback
  in this workflow, unlike CI).
- No release artifact is scoped by API level or ABI; a single debug APK is attached.

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

1. Commit the Gradle wrapper (or drop wrapper references from CI, release and CONTRIBUTING).
2. Make the release workflow assemble a release variant and actually sign it with cosign.
3. Derive `versionName` from the tag at build time instead of editing two files by hand.
4. Add a Room migration policy before shipping schema changes, because the current
   `fallbackToDestructiveMigration()` discards user data.
