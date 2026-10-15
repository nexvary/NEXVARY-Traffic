#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/libs app/src/main/assets/native-licenses
archive=app/libs/hev-socks5-tunnel-2.19.0.aar
if ! test -f "$archive"; then
  curl --fail --location --retry 3 --output "$archive" https://github.com/heiher/hev-socks5-tunnel/releases/download/2.19.0/hev-socks5-tunnel.aar
fi
printf '%s  %s\n' fbbf014c1c4c6d1e0922db88542abd9dc726600473bdfdc8039ed157b4ba02b0 "$archive" | sha256sum --check -
# Include upstream and bundled third-party notices in the distributed APK.
source_archive="$(mktemp)"
source_dir="$(mktemp -d)"
trap 'rm -f "$source_archive"; rm -rf "$source_dir"' EXIT
curl --fail --location --retry 3 --output "$source_archive" https://github.com/heiher/hev-socks5-tunnel/releases/download/2.19.0/hev-socks5-tunnel-2.19.0.tar.xz
printf '%s  %s\n' 6ee1db4f999708dd7fb04ff021c13b603dd2eae45791ea07493de3506778a788 "$source_archive" | sha256sum --check -
tar -xJf "$source_archive" -C "$source_dir" --strip-components=1
python3 - "$source_dir" <<'PY'
from pathlib import Path
import sys
root=Path(sys.argv[1])
out=Path("app/src/main/assets/native-licenses/HEV-2.19.0-NOTICES.txt")
notices=[]
for path in sorted(root.rglob("*")):
    if path.is_file() and (path.name.lower().startswith(("license", "copying", "copyright"))):
        notices.append(str(path.relative_to(root))+"\n"+path.read_text(errors="replace"))
if not notices:
    raise SystemExit("Missing native dependency notices")
out.write_text("\n\n".join(notices))
PY
