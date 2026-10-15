# NEXVARY Traffic Lab v8.0 — Verification Report

## Executed JVM regression tests
- SMOKE_OK — embedded localhost demo server returned HTTP 200 with modified authorized-lab request.
- STAGES_2_TO_10_OK
- STAGES_11_TO_20_OK
- STAGES_21_TO_30_OK
- STAGES_31_TO_40_OK
- STAGES_41_TO_50_OK
- V6_ROBUSTNESS_OK — UTF-8 and 24 concurrent local requests.
- AUTH_CRYPTO_OK — PBKDF2-HMAC-SHA256, 120000 iterations, 32-byte derived hash.
- STRUCTURED_BODY_EDITOR_OK — parsed and rebuilt structured JSON, including one-tap price mutation.
- V8_VISUAL_FLOW_OK — simulated Visual Editor mutation produced `price=1`, `qty=2`, sent to localhost demo server, and received HTTP 200.

## Static checks
- 7 locale resource sets parse as XML and have exact key parity.
- 146 string keys per locale.
- Visual Editor, RAW, quick mutation, Forward, Drop, Send & Compare and per-field control listeners were checked in source.
- Version code 80 / version name 8.0.0-lab.

## Environment limitation
Android SDK/android.jar is not installed in this execution environment, therefore an Android Gradle APK build and device/emulator visual rendering test could not be executed here. Core Java modules and the local-lab request flow were compiled and executed with javac/java.
