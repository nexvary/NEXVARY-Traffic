# Foundation review — 2026-10-15

## Provenance

Imported the latest matching Library source archive, v10.0.3, into the previously empty `nexvary/NEXVARY-Traffic` repository. Kept the existing Android application and tests. Excluded generated `.class` files and verification outputs. Historical reports are preserved as historical claims only.

## Change

`RequestNormalizer` now removes all case-insensitive Content-Length duplicates, computes one canonical UTF-8 length (including zero), handles null bodies, and preserves the original request. Added a regression test covering duplicate casing, Arabic and emoji bytes, empty/null input, unrelated headers, evidence preservation and idempotence.

Added CI for the JVM suite, Android debug assembly, lint and artifact collection. This does not establish production readiness.

## Verification in this session

- Android XML syntax: PASS.
- Seven locale resource key sets: PASS, 161 keys per locale.
- UTF-8 regression fixture: independently checked, 15 bytes.
- JVM suite: NOT RUN; Java runtime exists but `javac` is unavailable.
- Android assembly, lint and emulator/device tests: NOT RUN locally; SDK and Gradle are unavailable.
- GitHub Actions: consult the workflow run for this commit; local inspection cannot establish its result.

## Priorities after CI

P0: Obtain a successful APK build and execute device acceptance checks. Verify authentication, capture service permission/revocation/shutdown and Arabic layout.

P1: Bound the HTTP client's response buffer, the server task queue, and capture file growth; add focused regression tests for each change. The current source has a fixed worker count but an unbounded executor queue.

P1: Make capture scope and lack of packet forwarding explicit in the app interface, and define the intended product scope before implementing broader traffic capture.

P2: Split the large activity into page components after baseline behavior is tested; review translation coverage beyond key parity and finalize licensing.
