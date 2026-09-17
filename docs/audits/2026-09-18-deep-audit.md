# Deep Audit and Optimization Report

## Scope and evidence

This audit covered market-data correctness, indicator contracts, Android lifecycle and concurrency, Compose accessibility, privacy and network security, dependency/build configuration, CI quality gates, artifact safety, and documentation. Evidence came from source review, the existing regression suite, a five-domain independent review, focused Gradle verification, and a clean full Gradle run.

## Findings and disposition

| Priority | Finding | Disposition |
|---|---|---|
| High | `targetSdk = 34` is below the Android 16/API 36 Play target requirement for the 2026 release window. | Fixed by moving compile and target SDK to 36 and installing Platform/Build Tools 36 locally. |
| Medium | Nullable/empty Retrofit bodies could escape as runtime null failures instead of `FailureKind.MALFORMED`. | Fixed by making API bodies nullable and mapping null ticker/candle bodies at the repository boundary. |
| Medium | Successful analysis results were not announced to assistive technologies. | Fixed with a polite live region on the result card. |
| Low | Pair normalization used the default locale. | Fixed with trim plus `Locale.ROOT`, with a Turkish-locale regression test. |
| Low | Legacy backup policy was implicit through `allowBackup=false` only. | Fixed with explicit `fullBackupContent` exclusion in addition to Android 12+ extraction rules. |
| Low | Pair request test asserted only the ticker ID and used a misleading future-candle fixture. | Fixed by using a completed fixture and asserting symbol, timeframe, range, and successful result. |
| Low | README's local quality command did not mention instrumentation. | Fixed by distinguishing JVM/build gates from the emulator-backed CI gate. |
| Low | Pair selector semantics did not identify its purpose or expanded state. | Fixed with localized content and state descriptions. |

## Verification results

The focused repository and indicator tests plus `lintDebug` completed successfully. A clean full run completed successfully with `clean check lintDebug testDebugUnitTest assembleDebug assembleRelease :app:assembleDebugAndroidTest`. The run produced no compilation errors; the native symbol stripping notice for `libandroidx.graphics.path.so` is a packaging warning from a dependency and does not fail the build.

The local sandbox cannot boot an Android emulator because `/dev/kvm` is unavailable. The hosted GitHub Actions workflow remains the authoritative instrumentation environment. The workflow continues to run instrumentation before APK verification and upload, and the package verifier workaround is restricted to the ephemeral CI emulator.

## Residual risks and recommendations

The release APK remains unsigned by design. A production release still requires a protected upload keystore, GitHub Secrets, signed AAB generation, signature verification, and an approval-gated release workflow. Dependency upgrades should be handled as a separate controlled change with compatibility and instrumentation coverage. Additional ViewModel tests for rapid refresh, pair changes during in-flight requests, cancellation, and Activity recreation would strengthen lifecycle coverage, but the audit found no evidence of a current lifecycle defect.

The next product-level decision is whether gaps in market history should remain hard failures or be surfaced as an explicit data-quality warning. That choice changes user-facing signal semantics and should be approved as a domain policy change before implementation.
