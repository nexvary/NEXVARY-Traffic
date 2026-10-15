# Integrated lab — 10.1.0

This change replaces the PCAPdroid control screen with an in-process explicit HTTPS proxy. No PCAPdroid APK, Python runtime or remote capture service is required by this implementation.

## Implemented

- On-device CONNECT proxy at 127.0.0.1:8888, scoped to one configured upstream hostname and port.
- Device-local CA whose RSA signing key stays in AndroidKeyStore and cannot be exported. Only the public CA certificate can be exported through Android's document picker.
- Host-specific server certificates, upstream TLS certificate validation and HTTPS hostname verification.
- HTTP/1.1 request capture, pause, edit, forward-original, forward-edited and drop. Response forwarding and a bounded response preview.
- Bounded connections, queue, request/header size, flow history, certificate cache and response byte budget. Socket timeouts and a 120-second decision timeout.
- Foreground service with an explicit Stop action. Exit revokes the login session, stops both app services, closes the local demo server and removes the app task.
- Navigation tags and instrumented tests exercise all main navigation buttons, repeated tabs, back-stack behavior, HTTPS screen return, Arabic navigation, exit cancellation and confirmed exit.

## Protocol limits

The target application must use the explicit proxy and trust the lab CA. This is not yet a full device VPN forwarding engine. Certificate pinning bypass, QUIC/HTTP3, HTTP/2, WebSocket upgrades and chunked request uploads are not implemented. Response bodies are forwarded as received; the text preview can be compressed or binary. A connection handles one HTTP/1.1 request and requests upstream connection close.

The request editor is for UTF-8 text. Forward Original preserves the captured request body bytes, including binary content; Forward Edited encodes the edited body as UTF-8 and recalculates Content-Length. Ordinary routing fields and framing headers are normalized. No upstream trust-all or hostname-verification bypass is used in production.

The service owns temporary in-memory flow history; stopping it discards those flows. Captured timings measure upstream connection, TLS and response time and exclude time spent paused by the instructor. Completed request/response text previews are handed to a bounded process-local archive and imported into History/Evidence and their analysis pages when navigating or returning to MainActivity. The archive retains at most 32 exchanges, each preview capped at 32,768 characters; truncated evidence is tagged PREFIX_ONLY. The archive survives stopping the capture service within the same process; explicit app exit clears it. The generic VPN service remains limited to the earlier closed lab subnet and is not the HTTPS engine.

## Verification

CI builds the debug APK and runs lint. A separate Android 35 x86_64 emulator job executes real navigation and TLS integration tests. Read the run result for the specific commit; test source alone is not evidence that the tests passed.

The TLS tests create a local HTTPS fixture, trust only its test CA, decrypt and modify a request, assert the upstream receives the modified body, and verify the response reaches the client. Additional tests check drop-before-upstream, out-of-scope rejection, CA persistence/non-exportable key and production rejection of an untrusted upstream certificate. The fixture's trust context is passed only to the test constructor; the production service uses SSLContext.getDefault().

## Remaining before calling the application complete

1. Implement and test full VPN packet forwarding and per-app routing, including IPv6 and DNS, before claiming all-app capture.
2. Add HAR import/export and persistent retention controls beyond the bounded in-memory capture handoff.
3. Add response editing and supported streaming protocols with protocol-specific tests.
4. Validate CA setup and capture on physical lab devices and multiple Android versions.
5. Expand UI visual QA, translation coverage and accessibility. Existing lint warnings must be reviewed.

## Dependencies

Certificate construction uses Bouncy Castle bcpkix-jdk18on 1.86 and its transitive components. Preserve their bundled license notices when distributing. No NetBare source is included: its repository-level license metadata and source-file headers differ, and its older Android implementation needs a separate compatibility review.
