# IndodaxSignal

Aplikasi Android Kotlin/Jetpack Compose untuk menampilkan sinyal teknikal berbasis data OHLC Indodax.

## Toolchain Mandiri

Bootstrap seluruh toolchain pada Ubuntu dengan:

```bash
./scripts/setup-android-toolchain.sh
source .toolchain.env
```

Script memasang atau memastikan tersedia:

- OpenJDK 17;
- Android SDK command-line tools;
- Android SDK Platform 36;
- Android SDK Build-Tools 36.0.0;
- Android Platform-Tools;
- Android Emulator;
- `local.properties` yang menunjuk ke SDK lokal.

`.toolchain.env` dan `local.properties` bersifat lokal dan tidak boleh di-commit.

## Perintah Pengembangan

Setelah environment dimuat:

```bash
source .toolchain.env
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

Quality gate JVM/build lokal:

```bash
./gradlew --no-daemon clean check lintDebug testDebugUnitTest assembleDebug assembleRelease
```

Quality gate CI lengkap juga menjalankan emulator-backed instrumentation test dan verifikasi checksum sebelum upload artifact:

```bash
./gradlew --no-daemon connectedDebugAndroidTest
```

Untuk menjalankan emulator yang dibuat oleh bootstrap:

```bash
source .toolchain.env
emulator -avd indodax-api34 -no-snapshot -no-audio -no-boot-anim
adb wait-for-device
```

## Struktur Utama

- `app/src/main/java/com/indodax/signal/data` — API Indodax, DTO, repository, dan validasi OHLC.
- `app/src/main/java/com/indodax/signal/indicator` — EMA, RSI, MACD, dan volume.
- `app/src/main/java/com/indodax/signal/signal` — aturan BUY/SELL/HOLD dan skor konfirmasi.
- `app/src/main/java/com/indodax/signal/viewmodel` — lifecycle request dan state UI.
- `app/src/main/java/com/indodax/signal/ui` — Jetpack Compose UI.
- `app/src/test` — unit dan regression tests.
- `.claude/skills` — workflow review Android dan signal regression.
- `.claude/agents` — reviewer khusus Android dan domain signal.

## Catatan Domain

Histori candle lama diperbolehkan selama timestamp valid dan candle terbaru masih berada dalam freshness window. Skor konfirmasi bukan probabilitas profit yang terkalibrasi.
