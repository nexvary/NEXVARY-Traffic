# Integrated VPN development — 10.3.0

The APK now embeds HevSocks5Tunnel 2.19.0 and an authenticated loopback SOCKS5 gateway. It does not require another capture APK, root, Python or a remote proxy.

## Routing
- The user selects training packages and approves Android's VPN consent. Only these packages route through the tunnel; NEXVARY cannot select itself.
- IPv4 and IPv6 default routes, TCP forwarding, standard SOCKS5 UDP relay, and a mapped lab DNS resolver are configured.
- The NEXVARY UID stays outside the VPN allowlist; forwarding TCP and UDP sockets also call VpnService.protect before connecting.
- Connections for the configured HTTPS hostname and port are redirected to the existing internal CONNECT proxy while it is running. Other destinations are forwarded without decryption.
- The mapped DNS resolver preserves hostnames for SOCKS routing. Applications using cached IPs, hard-coded addresses or encrypted DNS can bypass hostname inspection; their connections remain forwarded. This is not universal TLS interception.
- CA trust is still required. Pinning, custom TLS and application-level encryption are not bypassed.
- The VPN routes UDP (including encrypted QUIC) but does not decrypt QUIC or HTTP/2. HTTP/1.1 remains the inspection protocol.

## Lifecycle and limits
Foreground notification offers Stop and opens the VPN screen. Revoke, Stop and application Exit shut down the native engine and gateway. The gateway accepts only loopback clients authenticated with a newly generated random credential. Its clients, worker threads, UDP destinations and native sessions are bounded. Configuration is app-private and deleted on service destruction. Process death releases all sockets and descriptors; services are not sticky.

## Build
Run `bash ci/prepare-native.sh` before Gradle. This downloads a fixed 2.19.0 AAR and checks its published SHA-256. The pinned source archive is also verified; its license files are packaged in APK assets. No binary is committed. The upstream AAR targets API 29; this version therefore requires Android 10 or later.

Upstream: https://github.com/heiher/hev-socks5-tunnel/tree/2.19.0 (MIT). Native dependencies include lwIP, libyaml and hev-task-system; preserve the bundled notices.

## Verification status
New JVM tests cover authenticated TCP IPv4/IPv6 forwarding, UDP round-trip and live-socket shutdown. A new Android test drives raw IPv4/IPv6 datagrams through the real native engine into a local UDP fixture, checks return packets and restart. CI results for the current commit, not the presence of these tests, determine whether validation passed. Physical-device testing is still outstanding.

## Real VPN PCAP capture

The optional capture mode inserts an AF_UNIX/SOCK_DGRAM bridge between Android TUN and the native engine, preserving individual IPv4/IPv6 packets in each direction. Without capture, the engine retains the original direct TUN path. Two bounded-buffer pump threads write complete DLT_RAW records to an app-private 32 MiB capture. A recording error or size limit stops capture without stopping routing; a transport pump failure stops the VPN and reports an error. Stop closes the native engine, bridge and capture before TUN. Export and delete require Stop. Exit deletes the temporary PCAP. Exported files are user-managed and contain unredacted packet data, including plaintext protocols; TLS payloads remain encrypted.
