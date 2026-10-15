#!/usr/bin/env bash
set -uo pipefail
adb shell wm dismiss-keyguard
adb logcat -c
python3 ci/network-fixture.py > fixture-network.log 2>&1 &
fixture_pid=$!
trap 'kill "$fixture_pid" 2>/dev/null || true' EXIT
gradle --no-daemon :app:installDebug :fixture:installDebug || exit $?
adb shell appops set com.nexvary.trafficlab ACTIVATE_VPN allow
adb push fixture-ca.der /data/local/tmp/nexvary-fixture-ca.der
gradle --no-daemon :app:connectedDebugAndroidTest
test_status=$?
adb logcat -d > device-logcat.txt
exit "$test_status"
