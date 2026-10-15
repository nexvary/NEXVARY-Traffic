#!/usr/bin/env bash
set -uo pipefail
adb shell wm dismiss-keyguard
adb logcat -c
gradle --no-daemon connectedDebugAndroidTest
test_status=$?
adb logcat -d > device-logcat.txt
exit "$test_status"
