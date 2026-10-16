# NEXVARY Traffic Lab v6.0 — Verification Report

## Scope
Internal authorized lecture/lab build. This verification pass focused on code correctness, local-lab robustness, Android configuration hardening, localization completeness, and regression safety across stages 2–50.

## Code review fixes applied
- Reworked `DemoLabServer` request parsing to read HTTP body by bytes (not characters), fixing UTF-8 `Content-Length` mismatches.
- Added 32 KiB header and 1 MiB body limits to the embedded loopback server.
- Added malformed request/content-length handling and safer request-line validation.
- Kept the embedded server bound to loopback only.
- Added fixed-size handler pool instead of unbounded cached worker creation.
- Hardened VPN capture loop so one malformed packet cannot terminate capture.
- Added foreground notification/channel for `LabVpnService` lifecycle compatibility.
- Disabled global cleartext declaration in the Android manifest; the current embedded lab uses raw loopback sockets.
- Kept `allowBackup=false`, VPN service non-exported, and launcher as the only exported component.
- Corrected stale dashboard version text from v4/stages 2–40 to v6/stages 2–50.
- Added explicit RTL support.

## Languages
Seven UI languages are included with persistent in-app selection:
- English (`en`)
- Arabic (`ar`, RTL)
- Turkish (`tr`)
- Russian (`ru`)
- Spanish (`es`)
- German (`de`)
- Italian (`it`)

Android resource parity check: all seven locale files contain the same 103 string keys. Primary navigation, screen titles, main actions and lecture-facing labels are localized. Protocol names and raw captured traffic remain in their technical/original form intentionally.

## Verification executed
- Full JVM compilation of non-Android production engines using Java 17 language level.
- Smoke test of embedded local server and request mutation scenario.
- Regression tests for stages 2–10, 11–20, 21–30, 31–40 and 41–50.
- UTF-8 HTTP body/content-length regression test.
- 24-request concurrent embedded-server stress check.
- XML parse/key-parity validation for all seven `strings.xml` locale files.
- Manifest security assertions: backup disabled, global cleartext disabled, RTL enabled, VPN service not exported.
- Static scan for runtime command execution, `ProcessBuilder`, dynamic class loading, WebView JavaScript enablement, world-readable modes and broad external-storage permissions.
- Java parser pass over Android-specific sources; Android symbols remain unresolved because this environment does not contain `android.jar`, but no source syntax error class was detected.

## Remaining verification boundary
A real Android Gradle build / lint / instrumentation run cannot be claimed in this environment because the Android SDK and Android Gradle runtime are not installed. The source package is prepared for Android Studio / AndroidIDE, where the final APK build should still be executed before the lecture device is used.
