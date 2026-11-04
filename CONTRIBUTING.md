# Contributing

## 1. Local setup

- **JDK 17** (CI uses Temurin 17).
- **Android SDK** able to compile `compileSdk 36`.
- **Gradle**: this repository does not commit a wrapper (`gradlew` and `gradle-wrapper.jar` are
  absent), so run an installed `gradle` or open the project in Android Studio. CI runs
  `gradle test --no-daemon` and `gradle assembleDebug --no-daemon`.
- Optional: `.env` for the Secrets Gradle plugin, seeded from `.env.example`.

```bash
gradle :core-engine:test
gradle :security-interface:test
gradle :app:testDebugUnitTest
gradle test          # what CI runs
```

## 2. Architectural guardrails

The numbers below match what `.github/workflows/ci.yml` actually enforces. C7 and C9 are conventions
implemented in code with no CI step; there is no C11–C13.

| ID | Rule | Enforced in CI |
| :--- | :--- | :--- |
| C1 | No runtime dependencies in `:core-engine` (`implementation(...)` must not appear) | yes |
| C2 | No `import android.` in `:core-engine/src/main` | yes |
| C3 | Dependency direction `:app` → `:security-interface` → `:core-engine` only | yes |
| C4 | `MiniJson` stays inside `:core-engine`; app code uses `JsonCodecPort` | yes |
| C5 | No secrets in source; gitleaks scans every push | non-blocking |
| C6 | Outbound HTTP goes through `HttpTransportPort`, never `HttpURLConnection` | yes |
| C7 | Tool executions emit telemetry through `ToolInvocationInterceptor` | convention |
| C8 | No `println`, `printStackTrace` or `System.out` in production source | yes |
| C9 | Telemetry failures must never break execution (`SafeTelemetryDecorator`) | convention |
| C10 | No Perchance remnants in `app/src/main` | yes |

Guardrails are `grep`-based: they match text, so avoid writing the forbidden literals inside strings
or comments.

## 3. Code expectations

- Keep `:core-engine` pure Java 11: no records, no `java.net.http`, no virtual threads, no Android
  imports, no third-party libraries.
- New engine capability means a new port interface plus an adapter in `:app` or
  `:security-interface` — never a dependency inside the engine.
- Classify every new tool in `ToolRiskRegistry`. Do not lower a level to skip approval, and validate
  paths through `FileGovernanceEngine.validatePath`.
- Fail closed: a missing approver, a thrown exception, or a mismatched approval request id must deny
  the call and still write an audit record.
- Add unit tests for new behaviour. Engine and security logic must have tests next to the class;
  network behaviour belongs in the MockWebServer harness described in `docs/e2e.md`.

## 4. Documentation expectations

- Update the relevant file under `docs/` when behaviour changes, and update
  `docs/file_index.md` (plus the matching part under `docs/file_index/`) when files are added,
  removed or renamed.
- Keep every authored document at or below **200 lines**. If a document grows past that, split it
  into focused files with cross-reference pointers and keep the parent as an index.
- Never document intended behaviour as if it shipped. Record gaps in `docs/status.md` with file and
  line evidence.
- Add changelog entries under `[Unreleased]` in `CHANGELOG.md`.

## 5. Pull request checklist

1. `CHANGELOG.md` updated under `[Unreleased]`.
2. Guardrail greps in `docs/ci.md` run locally and clean.
3. Unit tests pass for the modules you touched; if the app module does not compile for reasons outside
   your change, say so in the pull request description instead of leaving it implied.
4. Documentation updated as described in section 4.
5. No secrets, tokens or keystores added; `debug.keystore` is the only committed key material and it is
   for debug builds only.
