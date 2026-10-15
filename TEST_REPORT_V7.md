# NEXVARY Traffic Lab v7.0 — Verification Report

## UI / navigation review
- 35 primary screens were statically enumerated and every navigation target method was found.
- All programmatically-created Button/iconButton variables in MainActivity and LoginActivity have click listeners.
- The long module navigation remains in a horizontal scroller with flexible height, avoiding fixed-width crowding.
- The Intercept Forward/Drop row now uses equal-weight buttons; the long mutation action is on its own full-width row.
- Top bar was split so language control cannot crowd the title/back/lock controls.
- Android system-bar insets are applied on API 30+; older versions use safe base padding.
- Hardware Back is overridden: it walks the internal navigation stack and does not close the application from the root. A visible Back button uses the same logic.
- Arabic root layout, text and edit fields use RTL/right alignment.

## Authentication review
- LoginActivity is the only exported launcher activity; MainActivity is non-exported.
- MainActivity additionally requires an in-process AuthSession, preventing direct task restoration after process death from bypassing the lock.
- Passcodes are stored as PBKDF2-HMAC-SHA256 hashes, 120,000 iterations, 256-bit derived key, random 16-byte salt.
- Minimum policy: 6–128 chars and at least one letter + one number.
- 5 failed attempts trigger a 30-second local throttle.
- allowBackup=false remains enabled.
- Email recovery uses a configurable HTTPS service and intentionally stores no SMTP/API provider secret in the APK.

## Tests executed
- AUTH_CRYPTO_OK: correct password accepted, wrong password rejected, policy/email validation checked.
- SMOKE_OK: localhost request-modification lecture scenario.
- STAGES_2_TO_10_OK.
- STAGES_11_TO_20_OK.
- STAGES_21_TO_30_OK.
- STAGES_31_TO_40_OK.
- STAGES_41_TO_50_OK.
- V6_ROBUSTNESS_OK: UTF-8 body and 24 concurrent local requests (test name retained for regression continuity).
- I18N_PARITY_OK: 134 keys × 7 locales.
- MANIFEST_AUTH_HARDENING_OK.
- MainActivity BUTTON_LISTENERS_OK: 17 named programmatic controls.
- LoginActivity BUTTON_LISTENERS_OK: 3 named programmatic controls.
- NAV_BACK_RTL_OVERLAP_GUARDS_OK: 35 primary screens.
- AUTH_RECOVERY_FLOW_STATIC_OK.
- DANGEROUS_STATIC_SCAN_OK.
- ANDROID_SOURCE_PARSE_NO_SYNTAX_ERRORS.

## Verification boundary
This environment still has no Android SDK/android.jar/Gradle runtime, so a real APK build, Android Lint, instrumentation test, device rendering and touch test cannot honestly be claimed here. The source-level and JVM checks above passed; final APK UI rendering should be built once in Android Studio or AndroidIDE before the lecture.
