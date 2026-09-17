# Production Safety Specification

## Capability Map

| Module | Responsibility | Depends on |
|---|---|---|
| market-validation | Validate hourly cadence, gaps, and closed candles | Indodax API DTOs |
| signal-contract | Enforce indicator and signal invariants | market-validation |
| android-safety | Declare explicit backup/data-transfer policy | Android manifest |
| instrumentation | Verify Activity startup and accessible Compose smoke path | android-safety |
| ci-quality | Run JVM, lint, build, and instrumentation gates | all modules |

Build order: market-validation → signal-contract → android-safety → instrumentation → ci-quality.

## Objective

Prevent technically valid but semantically unsafe market data from producing misleading signals, and ensure Android runtime behavior is exercised in CI before artifacts are uploaded.

## Assumptions

1. The Indodax history endpoint returns candle timestamps representing the opening time of each candle.
2. The application requests one-hour candles (`tf=60`).
3. Signals should use only completed candles; an in-progress candle must not affect indicators or volume confirmation.
4. Production signing credentials are not available in this task and must never be generated or committed by automation.

## Acceptance Criteria

- Repository accepts only strictly hourly candle spacing for the hourly request.
- A candle whose close time is after `now` is excluded from signal input.
- The latest completed candle must remain within the existing freshness window.
- MACD cross-up and cross-down cannot both be true in a valid snapshot.
- RSI input shorter than its required period has an explicit failure contract.
- Android 12+ data extraction policy explicitly matches the no-backup intent.
- An instrumentation smoke test launches the main activity and verifies the primary UI content.
- CI runs instrumentation tests before APK verification and upload.
- No artifact upload occurs if any quality gate fails.
- Production signing remains a separately gated workflow requiring user-provided secrets.

## Verification Environment Notes

The local sandbox has the Android SDK, system image, and AVD installed, but does not expose `/dev/kvm`; therefore the local emulator cannot boot with hardware acceleration. Instrumentation verification is configured in GitHub Actions, where the hosted runner provides the required emulator capability. Production signing is intentionally not configured because no user-provided keystore or signing secrets are available.

## Testing Strategy

JVM tests cover candle cadence, gaps, open-candle filtering, RSI contract, MACD invariants, and signal conflict behavior. Android instrumentation tests cover Activity launch and core visible content. GitHub Actions runs all JVM/lint/build gates plus `connectedDebugAndroidTest` before artifact upload.

## Boundaries

- Always: keep market timestamps deterministic through injected clock; add regression tests before behavior changes; preserve cancellation semantics; run full Gradle gates before commit.
- Ask first: production keystore/signing secrets, Play Store publishing, or changes to signal policy that alter user-facing recommendations.
- Never: commit secrets, accept arbitrary timeframe data as hourly, upload APKs before tests pass, or use debug signing for release.

## Open Questions

- Whether the product should surface a data-quality warning when history has gaps rather than rejecting it.
- Whether production release should use Play App Signing with a signed AAB.
