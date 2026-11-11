#!/usr/bin/env bash
set -euo pipefail
mkdir -p device-results
collect_results() {
  adb logcat -d > device-results/logcat.txt || true
  adb shell dumpsys activity activities > device-results/activities.txt || true
  adb exec-out screencap -p > device-results/final-screen.png || true
  adb pull /sdcard/Android/data/de.meiapps.sternenreise/files/. device-results/ || true
}
trap collect_results EXIT
adb install -r "$(find test-apks -name app-github-debug.apk -print -quit)"
adb install -r "$(find test-apks -name app-github-debug-androidTest.apk -print -quit)"
adb shell settings put secure immersive_mode_confirmations confirmed
adb logcat -c
adb shell am instrument -w de.meiapps.sternenreise.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/instrumentation.txt
grep -Eq 'OK \([0-9]+ tests?\)' device-results/instrumentation.txt
for image in menu phone stereo alignment open-chest garden sprout flower observatory ending; do
  adb shell test -f "/sdcard/Android/data/de.meiapps.sternenreise/files/$image.png"
done
adb logcat -d > device-results/logcat.txt
if grep -E 'FATAL EXCEPTION|Fatal signal' device-results/logcat.txt; then exit 1; fi
