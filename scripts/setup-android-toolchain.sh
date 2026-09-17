#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
CMDLINE_TOOLS_VERSION="11076708"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

export DEBIAN_FRONTEND=noninteractive

if ! command -v javac >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y openjdk-17-jdk unzip
fi

mkdir -p "$SDK_ROOT/cmdline-tools" "$SDK_ROOT/licenses"
if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "$tmp_dir"' EXIT
  curl --fail --location --retry 3 "$CMDLINE_TOOLS_URL" -o "$tmp_dir/commandlinetools.zip"
  rm -rf "$tmp_dir/extracted" "$SDK_ROOT/cmdline-tools/latest"
  mkdir -p "$tmp_dir/extracted"
  unzip -q "$tmp_dir/commandlinetools.zip" -d "$tmp_dir/extracted"
  mv "$tmp_dir/extracted/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$PATH"
JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
export JAVA_HOME

yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator"

printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$ROOT_DIR/local.properties"
cat > "$ROOT_DIR/.toolchain.env" <<EOF
export JAVA_HOME="$JAVA_HOME"
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$JAVA_HOME/bin:$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:\$PATH"
EOF

printf '\nToolchain ready. Load it with:\n  source .toolchain.env\n\nSDK: %s\nJava: %s\n' "$SDK_ROOT" "$JAVA_HOME"
