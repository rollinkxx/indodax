---
name: android-pr-check
description: Use when reviewing or preparing Android Kotlin changes in this repository, especially before a pull request or release build.
---

# Android PR Check

Run this repository's quality gates in order and report each result.

## Workflow

1. Read `git diff` and `git diff --check`; never hide unrelated user changes.
2. Confirm Android SDK availability (`ANDROID_HOME` or `local.properties`).
3. If SDK exists, run:
   ```bash
   ./gradlew --no-daemon clean check lintDebug testDebugUnitTest assembleDebug assembleRelease
   ```
4. If SDK is missing, report the exact blocker; do not claim tests passed.
5. Inspect manifest, network security, dependencies, and release signing assumptions.
6. Check that CI installs `platforms;android-34` and `build-tools;34.0.0`.
7. Report findings as Critical/Major/Minor, then list passing and blocked gates.

## Repository Conventions

- Source packages: `data`, `indicator`, `signal`, `viewmodel`, `ui`.
- Network calls belong in `MarketRepository`; cancellation must rethrow `CancellationException`.
- Candle data must have a fresh latest candle; historical candles may be older.
- Signal scores are confirmation scores, not calibrated profit probabilities.
- Do not add secrets, `local.properties`, `.gradle/`, or build outputs to commits.

## Review Guardrails

- Prefer a regression test before changing production logic.
- Treat a failed environment prerequisite separately from an application failure.
- Do not weaken tests merely to make CI green.
