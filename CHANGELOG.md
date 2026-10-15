# v8.0.1-lab — Build Fix

- Fixed Android Gradle Plugin 8.x sync/build failure caused by custom `buildConfigField` while BuildConfig generation was disabled.
- Enabled `android.buildFeatures.buildConfig = true` in the app module.
- Bumped versionCode to 81 and versionName to 8.0.1-lab.

# Changelog

## v7.0.0-lab
- UI collision audit and responsive toolbar/navigation refinements.
- Gold + electric-green + emerald visual palette and local vector icons.
- Visible Back control plus non-exiting Android Back navigation.
- Explicit Arabic RTL alignment for UI text and inputs.
- Added local passcode gate with PBKDF2-HMAC-SHA256 salted hashing.
- Added failed-attempt throttling and process-local authenticated session guard.
- Launcher moved to LoginActivity; MainActivity is non-exported.
- Added immediate Lock action.
- Added backend-neutral HTTPS email recovery request/verify flow, without embedding provider secrets.
- Added 31 authentication/recovery strings to all 7 locales (134 keys per locale).
- Added AuthCrypto JVM regression test and static button/navigation/manifest audits.

# Changelog

## v6.0.0-lab
- Deep code-review and robustness pass.
- Fixed UTF-8 HTTP request-body parsing in the embedded loopback lab server.
- Added request-size limits, malformed request handling and bounded server concurrency.
- Hardened VPN packet loop and foreground-service lifecycle handling.
- Manifest hardening: backup disabled, global cleartext disabled, RTL enabled, VPN service non-exported.
- Added persistent seven-language UI selector: English, Arabic, Turkish, Russian, Spanish, German, Italian.
- Added matching Android resource sets for all seven languages.
- Localized primary navigation, lecture screens, core controls and major user-facing screen titles.
- Corrected stale dashboard version/stage text.
- Added `V6RobustnessTest` with UTF-8 body and 24-request concurrency coverage.
- Re-ran all stage regression tests 2–50 successfully.

## v8.0.0-lab — Visual Smart Request Editor
- Added a default Visual Editor for intercepted flat JSON bodies used by the authorized localhost lecture lab.
- Numeric values now have + / − controls and one-tap presets such as `price = 1`.
- String values expose preset buttons (original/demo/TE70/test) so the lecturer can demonstrate changes without typing JSON.
- Boolean values have one-tap TRUE/FALSE switching.
- Added Visual ↔ RAW switching while preserving manual RAW edits.
- Added Quick Mutations: Price 100→1, Quantity +1, Restore original.
- Added Send & Compare action to preserve the original request as the comparison baseline.
- Added a dependency-free StructuredBodyEditor with round-trip and numeric mutation tests.
- Added translations for the new UI across Arabic, English, Turkish, Russian, Spanish, German and Italian.

## v9.0.0-lab — Experience Pack
- Added Schema-Aware Visual Editor control inference.
- Added live Original ↔ Modified request comparison while editing.
- Added UTF-8-correct automatic Content-Length normalization before local transmission.
- Added Session Story Mode for presenter-friendly chronological narratives.
- Added One-Tap Lecture Mode that clears prior session history, loads the local order demo, and opens the visual intercept workflow.
- Added seven-language strings for the v9 experience features.
- Added V9ExperienceTest and kept the full regression suite from earlier stages.

## v10.0.0-ui-lab — Visual Reconstruction
- Rebuilt the application shell to follow the approved NEXVARY reference more closely: deep navy/black canvas, electric cyan accents, silver typography and restrained gold/emerald status colors.
- Added a fixed five-item bottom navigation bar: Dashboard, Intercept, History, Cockpit, About.
- Replaced the crowded primary navigation with a compact secondary advanced-tool ribbon.
- Added NEXVARY hero branding, capture-status metrics, 2x3 Quick Tools grid, Lecture Mode card and Recent Activity area.
- Added a full About page with contact rows in this exact order: Website, Email, Facebook, YouTube, X (Twitter).
- Added working Android intents for website/social links and mailto email.
- Added explicit Exit App button with confirmation and finishAffinity(); Android Back continues to navigate internally and does not exit at home.
- Added dedicated vector icons for Intercept, History, Repeater, Flow Studio, About, Website, Email, Facebook, YouTube and X.
- Added About/Exit/fallback strings to all seven locales and retained RTL behavior for Arabic.
- versionCode 100 / versionName 10.0.0-ui-lab.

## v10.0.1-fixed-lab
- Fixed MainActivity compilation failure caused by malformed multiline Arabic Java string in About page.
- Moved About text into localized Android resources.
- Forced installed application name to English: `NEXVARY Traffic Lab` across all locales.
- Added expressive NEXVARY Traffic Lab launcher/round icon resource and manifest wiring.
- Re-ran full JVM regression suite, XML/i18n checks and Java syntax scan.

## 10.0.3-build-fix-lab
- Restored `MainActivity.sampleOrder()` accidentally dropped during the v10 UI rebuild.
- Resolves all 9 `cannot find symbol method sampleOrder()` errors shown by Android Studio.
- Re-ran the complete JVM regression suite and Android resource/static validation after the fix.
