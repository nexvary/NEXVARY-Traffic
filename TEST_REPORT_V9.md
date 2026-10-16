# NEXVARY Traffic Lab v9.0 — Verification Report

## Executed JVM regression suite
The following tests were compiled from source with Java 17 and executed successfully:
- AuthCryptoTest
- SmokeTest
- Stage2To10Test
- Stage11To20Test
- Stage21To30Test
- Stage31To40Test
- Stage41To50Test
- V6RobustnessTest
- StructuredBodyEditorTest
- V8VisualFlowTest
- V9ExperienceTest

V9ExperienceTest specifically verifies:
- schema-aware control inference;
- original/modified change detection;
- UTF-8 byte-accurate Content-Length normalization;
- session-story generation.

## Android resource/static checks
- 7 locales have matching resource keys: 153 keys each.
- AndroidManifest.xml parses successfully.
- MainActivity.java has no Java parser-level syntax diagnostics in javac pre-typecheck parsing.
- Static scan found no Runtime.exec, ProcessBuilder, DexClassLoader, PathClassLoader, MODE_WORLD_*, MANAGE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, or QUERY_ALL_PACKAGES usage.

## Android APK build limitation
This environment does not contain Android SDK/android.jar, so the Android Gradle Plugin APK build and emulator UI run cannot be truthfully claimed as executed here. The project remains configured for compileSdk/targetSdk 35 and Java 17.
