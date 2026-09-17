# Production Safety Implementation Plan

> **For agentic workers:** Implement task-by-task with a focused test cycle after each task.

**Goal:** Make hourly market-signal inputs semantically safe and prevent CI from uploading APK artifacts unless JVM, lint, build, and Android instrumentation gates pass.

**Architecture:** Keep validation at the repository boundary, keep indicator invariants in the domain layer, declare Android backup policy in resources, and run a small instrumentation smoke test in a separate CI gate before artifact verification/upload. Production signing is intentionally excluded because it requires protected user-provided secrets.

**Tech Stack:** Kotlin, Android SDK 34, Jetpack Compose, JUnit4, AndroidX Test, Gradle 8.7, GitHub Actions.

**Spec:** `docs/specs/production-safety.md`

## Global Constraints

- Hourly request uses 3,600-second candle spacing.
- Only completed candles participate in signal calculation.
- No secrets or keystores may enter the repository.
- Artifact upload must remain after all verification gates.
- Preserve coroutine cancellation and existing failure categories.

---

### Task 1: Hourly cadence and closed-candle validation

**Files:**
- Modify: `app/src/main/java/com/indodax/signal/data/Data.kt`
- Modify: `app/src/test/java/com/indodax/signal/MarketRepositoryTest.kt`

- [ ] Add tests for one-minute spacing rejection, internal two-hour gap rejection, and an in-progress final hourly candle being excluded while a completed candle remains usable.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests 'com.indodax.signal.MarketRepositoryTest'` and confirm new tests fail before implementation.
- [ ] Implement deterministic cadence validation and completed-candle filtering using injected `nowSeconds`.
- [ ] Preserve latest-completed freshness and insufficient-history behavior.
- [ ] Run focused tests, then `./gradlew test lintDebug`.

### Task 2: Indicator and signal contracts

**Files:**
- Modify: `app/src/main/java/com/indodax/signal/indicator/Indicators.kt`
- Modify: `app/src/main/java/com/indodax/signal/signal/SignalEngine.kt`
- Modify: `app/src/test/java/com/indodax/signal/IndicatorsTest.kt`

- [ ] Add tests for RSI insufficient history, mutually exclusive MACD flags, and contradictory HOLD confirmations.
- [ ] Run focused tests and confirm the new contract tests fail before implementation.
- [ ] Implement explicit validation without changing normal BUY/SELL behavior.
- [ ] Run indicator and signal tests, then the full JVM suite.

### Task 3: Android data extraction rules

**Files:**
- Create: `app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Define no-backup/no-transfer policy for Android 12+.
- [ ] Reference it from the application manifest.
- [ ] Run lint and inspect the manifest-related report.

### Task 4: Instrumentation smoke test

**Files:**
- Create: `app/src/androidTest/java/com/indodax/signal/MainActivitySmokeTest.kt`
- Modify: `app/build.gradle.kts` only if a missing AndroidX test dependency is identified.

- [ ] Launch `MainActivity` with `ActivityScenario`.
- [ ] Assert the application label or primary screen text is visible through Compose semantics/UI tree.
- [ ] Run `./gradlew connectedDebugAndroidTest` with the local `indodax-api34` emulator.

### Task 5: CI instrumentation gate

**Files:**
- Modify: `.github/workflows/android.yml`

- [ ] Add emulator setup and `connectedDebugAndroidTest` before APK verification/upload.
- [ ] Ensure emulator cleanup runs even on failure.
- [ ] Keep artifact upload after all gates and `if-no-files-found: error`.
- [ ] Validate workflow YAML and run GitHub Actions.

### Task 6: Final verification and integration

**Files:** all files above.

- [ ] Run `git diff --check`, secret scan, full Gradle quality gates, and instrumentation tests.
- [ ] Review staged diff and commit atomic changes.
- [ ] Push branch/main according to repository workflow.
- [ ] Confirm GitHub Actions success and artifact contents.
- [ ] Document remaining production-signing requirement; do not fabricate signing secrets.
