#!/usr/bin/env bash
# Smoke test of the Android package on a running emulator or device:
# install the APK from dist/, launch it, check that the process stays alive and does not crash.
# (A release build is not debuggable, so files in its data folder cannot be inspected with run-as.)
set -euo pipefail

APP_ID="${APP_ID:-com.glyphdraft.app}"   # keep in sync with applicationId in androidApp (decided in M0)
APK="$(ls dist/*.apk | head -n 1)"

echo "Installing $APK"
adb install -r "$APK"

adb logcat -c
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1
sleep 8

if ! adb shell pidof "$APP_ID" > /dev/null; then
  echo "App process is not running"
  adb logcat -d | tail -n 80
  exit 1
fi

if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "App crashed"
  adb logcat -d | grep -A 20 "FATAL EXCEPTION"
  exit 1
fi

echo "Android smoke test passed"
