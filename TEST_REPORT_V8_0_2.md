# NEXVARY Traffic Lab v8.0.2 — Verification Report

Verification performed in the assistant execution environment before delivery.

## Build configuration
- Confirmed `buildFeatures { buildConfig true }` is present.
- Confirmed `RECOVERY_ENDPOINT` BuildConfig field is defined.
- compileSdk 35 / targetSdk 35 / minSdk 26.
- Java source/target compatibility 17.

## Executed JVM regression suite
All non-Android engines were compiled from source with javac and all available regression tests were executed successfully:
- AUTH_CRYPTO_OK
- SMOKE_OK
- STAGES_2_TO_10_OK
- STAGES_11_TO_20_OK
- STAGES_21_TO_30_OK
- STAGES_31_TO_40_OK
- STAGES_41_TO_50_OK
- V6_ROBUSTNESS_OK (UTF-8 + 24 concurrent requests)
- STRUCTURED_BODY_EDITOR_OK
- V8_VISUAL_FLOW_OK

## Android/static integrity checks
- 16 Android XML resource/manifest files parsed successfully.
- 7 locale resource sets checked for exact key parity.
- 146 string keys per locale.
- Manifest baseline verified: allowBackup=false, cleartext disabled, RTL support enabled.
- MainActivity exported=false.
- VPN service exported=false and protected with BIND_VPN_SERVICE.
- Launcher LoginActivity is the only intentionally exported activity.
- Dangerous API/pattern scan passed for Runtime.exec, ProcessBuilder, dynamic class loaders, world-readable modes, broad storage permissions and QUERY_ALL_PACKAGES.
- Java parser heuristic found no syntax-level errors in MainActivity, LoginActivity or LabVpnService; unresolved Android SDK symbols are expected because android.jar is not installed in this execution environment.

## Packaging
- ZIP integrity checked after generation.

## Remaining environment limitation
A full Android Gradle Plugin APK build and emulator/device UI run could not be executed in this environment because the Android SDK/android.jar is not installed. This is the only remaining verification layer; all tests possible without the SDK were run here before delivery.
