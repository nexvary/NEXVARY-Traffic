#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/.verify_classes"
rm -rf "$OUT" && mkdir -p "$OUT"
find "$ROOT/app/src/main/java/com/nexvary/trafficlab/core" "$ROOT/app/src/main/java/com/nexvary/trafficlab/evidence" "$ROOT/app/src/main/java/com/nexvary/trafficlab/packet" -name '*.java' > "$OUT/sources.txt"
printf '%s\n' "$ROOT/app/src/main/java/com/nexvary/trafficlab/vpn/LocalSocksGateway.java" "$ROOT/app/src/main/java/com/nexvary/trafficlab/vpn/TunnelConfig.java" >> "$OUT/sources.txt"
javac --release 17 -encoding UTF-8 -d "$OUT" @"$OUT/sources.txt"
javac --release 17 -encoding UTF-8 -cp "$OUT" -d "$OUT" "$ROOT"/testbuild/*.java
for t in PcapCaptureTest ResponseEditorTest HarExportTest Http1BodyTest SocksGatewayTest CapturedTrafficTest AndroidCompatibilityRegressionTest RequestNormalizerRegressionTest AuthCryptoTest SmokeTest Stage2To10Test Stage11To20Test Stage21To30Test Stage31To40Test Stage41To50Test V6RobustnessTest StructuredBodyEditorTest V8VisualFlowTest V9ExperienceTest; do
  java -cp "$OUT" "$t"
done

