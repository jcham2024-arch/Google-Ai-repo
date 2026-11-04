# Continuous Integration and Guardrails

The pipeline lives in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

- Triggers: pushes to `main` and `phase/**`, pull requests targeting `main`.
- Runner: `ubuntu-latest`, JDK 17 (Temurin), Android SDK installed by
  `android-actions/setup-android@v3`.
- Job: `validate-and-test`.

## Pipeline steps, in order

| Step | Command or action | Blocking |
| :--- | :--- | :--- |
| Checkout | `actions/checkout@v4` | yes |
| JDK 17 | `actions/setup-java@v4`, Gradle cache enabled | yes |
| Android SDK | `android-actions/setup-android@v3` | yes |
| Guardrail C1 | `grep -E '^\s*implementation\('` in `core-engine/build.gradle.kts` must not match | yes |
| Guardrail C2 | `grep -rn "import android\." core-engine/src/main/` must not match | yes |
| Guardrail C3 | `project(":app")` must not appear under `core-engine/` or `security-interface/`; `project(":security-interface")` must not appear under `core-engine/` | yes |
| Guardrail C4 | `import com.apex.core.json.MiniJson` must not appear in `app/src/main/` | yes |
| Guardrail C6 | `HttpURLConnection` must not appear in `app/src/main/` | yes |
| Guardrail C8 | `System.out`, `println` or `printStackTrace` must not appear in `core-engine/src/main/`, `security-interface/src/main/`, `app/src/main/` | yes |
| Guardrail C10 | `grep -rn -i "perchance\|perchance_generator" app/src/main/` must not match | yes |
| Guardrail C5 | `gitleaks/gitleaks-action@v2` secret scan | **no** (`continue-on-error: true`) |
| Tests | `gradle test --no-daemon` | yes |
| Build | `gradle assembleDebug --no-daemon` | yes |

The invocation lines also run `chmod +x ./gradlew || true` before calling bare `gradle`. That line is
vestigial: the repository has no Gradle wrapper. See section "Known issues" below.

## Guardrail numbering: documented vs implemented

The header comment above the guardrail block says "C1 - C8". The steps that exist are C1, C2, C3, C4,
C6, C8, C10 and C5. The table below is the accurate mapping; C7, C9, C11, C12 and C13 exist only as
conventions, not as CI steps.

| ID | Rule | Enforced in CI | Enforced in code |
| :--- | :--- | :--- | :--- |
| C1 | `:core-engine` has no runtime dependencies (allow-list in `core-engine/allowed-deps.txt`) | yes | — |
| C2 | `:core-engine` imports no Android framework classes | yes | — |
| C3 | Layer direction `:app` → `:security-interface` → `:core-engine` | yes | — |
| C4 | `MiniJson` stays inside `:core-engine` | yes | `JsonCodecPort` boundary |
| C5 | No secrets in source control | gitleaks, non-blocking | Keystore-backed storage, `.env` ignored |
| C6 | Outbound HTTP goes through `HttpTransportPort` | yes | `OkHttpTransport` |
| C7 | Tool executions emit telemetry | no | `ToolInvocationInterceptor` |
| C8 | No raw print statements | yes | `TelemetryPort` logging |
| C9 | Telemetry failures never break execution | no | `SafeTelemetryDecorator` |
| C10 | No Perchance remnants in `app/src/main` | yes | removed from source |

## Running the same checks locally

```bash
# C1 — expect no output
grep -n "implementation(" core-engine/build.gradle.kts
# C2 — expect no output
grep -rn "import android\." core-engine/src/main/
# C3 — expect no output
grep -rn 'project(":app")' core-engine/ security-interface/
grep -rn 'project(":security-interface")' core-engine/
# C4 — expect no output
grep -rn "import com.apex.core.json.MiniJson" app/src/main/
# C6 — expect no output
grep -rn "HttpURLConnection" app/src/main/
# C8 — expect no output
grep -rn "System\.out\|println\|printStackTrace" core-engine/src/main/ security-interface/src/main/ app/src/main/
# C10 — expect no output
grep -rn -i "perchance\|perchance_generator" app/src/main/
```

All of these checks produce no output against the current tree.

## Known issues

1. No Gradle wrapper is committed (`gradlew`, `gradle-wrapper.jar` are absent), while `ci.yml`,
   `release.yml` and `CONTRIBUTING.md` reference `./gradlew`. The workflow happens to work because it
   also calls bare `gradle`, which the runner provides; the `chmod +x ./gradlew || true` line is a
   no-op. Either commit the wrapper or remove the references.
2. Guardrails are `grep`-based and match text, not semantics. C8 fails on any line containing the
   substring `println` (a comment or a JSON key would trip it), and C10 only looks for the word
   `perchance` inside `app/src/main`, so other directories and other generator names are unchecked.
3. C5 cannot fail the build, so a leaked secret would only appear in the step log.
4. The app module currently contains unresolved references (see [status.md](status.md)), so the
   `gradle test` and `gradle assembleDebug` steps are expected to fail until those are fixed. CI
   status therefore does not currently indicate documentation accuracy.
