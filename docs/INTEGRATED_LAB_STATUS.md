# Integrated lab — 10.2.0 development

The Android APK contains its own native VPN transport, authenticated SOCKS5 gateway, HTTPS proxy and device-local certificate authority. No external capture APK, Python runtime or remote proxy is required. This version requires Android 10 (API 29) or later because of the native dependency's declared minimum API.

## Implemented

- User-selected training applications and Android VPN consent; TCP, UDP, IPv4 and IPv6 default routing. A mapped DNS resolver preserves destination hostnames for inspection routing. The NEXVARY UID is excluded from the allowlist; outbound sockets are also protected against routing loops.
- HevSocks5Tunnel 2.19.0 is packaged inside the APK. Build preparation verifies the AAR and source archive SHA-256 values and bundles upstream and dependency license notices. See VPN_INTEGRATION.md.
- HTTPS CONNECT inspection for one configured hostname and port, via the explicit proxy or the VPN's mapped DNS path. AndroidKeyStore retains the non-exportable CA signing key. Upstream certificate and hostname validation remain enabled.
- HTTP/1.1 request pause, edit, forward-original and drop, with Content-Length and chunked upload framing. Chunked bodies are decoded and normalized to Content-Length before forwarding and editing. Request body limit: 1 MiB. Expect negotiation remains unsupported.
- Optional response pause, forward-original, drop and editing of uncompressed UTF-8 text. Paused responses are limited to 1 MiB. Ordinary streaming responses retain a 16 MiB transaction limit and a 64 KiB preview. Compressed/binary responses can be forwarded unchanged or dropped; they are not offered for editing.
- A built-in offline HTTPS lesson runs a loopback TLS server and client, pauses a real request, permits editing and records the result. Its isolated trust context trusts only the device lab CA; production external upstream trust is unchanged.
- Completed text previews feed History, Evidence and analysis pages. Archive handoff retains 32 exchanges with 32,768-character previews; truncation is labeled. Request and response instructor pauses are excluded from upstream timing.
- HAR 1.2 export/import with NEXVARY preview extensions. Export hides bodies, query values and sensitive headers by default; full preview export is explicit. Imported/restored records carry separate provenance tags and are conservatively marked as preview-only. Imports are offline analysis and never replay traffic.
- Optional encrypted local session save/restore/delete. AES-GCM uses a non-exportable AndroidKeyStore key. Exit clears the temporary archive; explicitly saved archives remain until deleted. No automatic retention is enabled. Files are limited to 5 MiB and 32 imported exchanges.
- Navigation and exit tests cover main pages, Back behavior, Arabic navigation, HTTPS return, VPN/archive return, cancel Exit and confirmed Exit. Confirmed Exit revokes authentication, shuts down services and listeners, closes the demo server and removes the task.

## Protocol and capture limits

VPN forwarding does not make TLS universally decryptable. Applications must trust the lab CA. Certificate pinning, custom TLS, application-level encryption and end-to-end encryption are not bypassed. Applications using hard-coded or cached IPs or encrypted DNS can bypass hostname inspection, while their connections are forwarded. QUIC and HTTP/2 can travel through the VPN but are not decoded by the HTTP/1.1 inspector; WebSocket upgrades are not inspected.

HAR stores metadata and explicitly labeled text previews, not byte-exact packet bodies. The current Packet Lab PCAP button still writes an educational sample; the native VPN transport does not yet expose full packet PCAP export. All protocols are not claimed to be decoded. Persistent archive restoration requires an unlocked session and explicit action. Larger streaming protocol support and physical-device verification remain outstanding.

## Verification

JVM suites include real authenticated TCP IPv4/IPv6 and UDP round-trips, shutdown, request framing, response editing, archive bounds and redaction. Android tests drive raw IPv4/IPv6 datagrams through the native engine, and a separate-UID fixture application generates TCP, UDP and mapped DNS through the actual Android VPN. Another separate-app test routes HTTPS through VPN, edits a request and checks the verified local TLS upstream result. Additional tests cover real TLS request modification, response edit/drop, production upstream-trust rejection, encrypted archive round-trip/tamper rejection and UI lifecycle.

Read the CI result for the exact commit. Test source alone is not a passing result. No physical-device run has been performed in this environment.
