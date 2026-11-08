#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

COMPONENT="com.aamir.reelcounter/com.aamir.reelcounter.service.ReelTrackerService"

echo "==> Building debug APK..."
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
echo "==> Installing (no force-stop — Oppo kills a11y after force-stop)..."
adb install -r "$APK"

# Warm the process first so Athena is less likely to kill a cold a11y bind.
echo "==> Warming app process..."
adb shell am start -n com.aamir.reelcounter/.ui.MainActivity >/dev/null 2>&1 || true
sleep 2

echo "==> Re-enabling accessibility service..."
adb shell settings delete secure enabled_accessibility_services >/dev/null 2>&1 || true
adb shell settings put secure accessibility_enabled 0
sleep 1
adb shell settings put secure enabled_accessibility_services "$COMPONENT"
adb shell settings put secure accessibility_enabled 1
sleep 2

echo "Done."
echo "If the counter is still missing: Settings → Accessibility → ReelCounter → Off → On"
echo "Also set Battery → Unrestricted for ReelCounter (stops Oppo Athena kills)."
