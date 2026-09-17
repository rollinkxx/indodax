---
name: signal-regression
description: Use when changing market-data validation, EMA, RSI, MACD, volume rules, signal scoring, or their Kotlin tests in this repository.
---

# Signal Regression

Protect domain behavior with small deterministic tests before changing signal logic.

## Required Cases

- Repository: unsupported pair, empty history, malformed ticker, future timestamp, stale latest candle, old-but-valid history, unsorted timestamps, invalid OHLC, negative/invalid volume, and oversized payload.
- Indicators: insufficient history, flat/rising/falling RSI, EMA seed and warm-up, MACD line/signal/crossover, zero volume, current-volume exclusion from baseline, finite output, and timestamp order.
- Signal engine: BUY, SELL, HOLD, boundary RSI 30/70, volume above/equal/below baseline, partial confirmation score, and no-volume HOLD.

## Workflow

1. Add the smallest failing test at the repository or domain seam.
2. Run the focused Gradle test.
3. Apply the smallest production change.
4. Run the focused test, then the full unit test suite.
5. Compare EMA/MACD golden vectors when definitions or warm-up behavior change.
6. Keep scores described as confirmation scores unless calibration evidence exists.

## Commands

```bash
./gradlew :app:testDebugUnitTest --tests 'com.indodax.signal.*'
./gradlew test lintDebug
```

If Android SDK is unavailable, report the prerequisite failure and do not infer test results.
