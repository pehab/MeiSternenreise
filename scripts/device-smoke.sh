#!/usr/bin/env bash
set -euo pipefail
mkdir -p device-results
adb install -r "$(find test-apks -name app-github-debug.apk -print -quit)"
adb install -r "$(find test-apks -name app-github-debug-androidTest.apk -print -quit)"
adb shell settings put secure immersive_mode_confirmations confirmed
adb logcat -c
adb shell am instrument -w de.meiapps.sternenreise.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/instrumentation.txt
grep -q 'OK (1 test)' device-results/instrumentation.txt
adb pull /sdcard/Android/data/de.meiapps.sternenreise/files/menu.png device-results/menu.png
adb pull /sdcard/Android/data/de.meiapps.sternenreise/files/phone.png device-results/phone.png
adb pull /sdcard/Android/data/de.meiapps.sternenreise/files/stereo.png device-results/stereo.png
adb logcat -d > device-results/logcat.txt
if grep -E 'FATAL EXCEPTION|Fatal signal' device-results/logcat.txt; then exit 1; fi
