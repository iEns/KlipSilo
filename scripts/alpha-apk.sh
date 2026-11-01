#!/usr/bin/env bash
# Build a KlipSilo alpha APK, copy it into the APK folder under the usual flat
# name, and write a sidecar (<file>.apk.json) next to it.
#
#   scripts/alpha-apk.sh [debug|beta]      (default: debug)
#
# Names (unchanged from the hand-built ones):
#   debug -> KlipSilo-<sha>-debug.apk
#   beta  -> KlipSilo-<sha>-beta-minified.apk
#
# This script only runs the normal Gradle task and adds a JSON file. It does
# not set DEPLOYTOOL_ARTIFACT_REF or DEPLOYTOOL_SIGNING_*, and it does not touch
# the versionCode scheme, signing, the R8 mapping output, or the website beta
# cut (that stays in klipsilo-ops deploy/site.toml).
#
# Env:
#   APK_DIR       where APKs go (default: ~/apks)
#   COVE_SIDECAR  path to the sidecar helper (apk-sidecar.py)
#                 (default: ~/projects/cove/scripts/apk-sidecar.py)
set -euo pipefail

variant="${1:-debug}"
case "$variant" in
  debug) task=":app:assembleDebug"; suffix="debug" ;;
  beta)  task=":app:assembleBeta";  suffix="beta-minified" ;;
  *) echo "usage: $0 [debug|beta]" >&2; exit 2 ;;
esac

repo="$(cd "$(dirname "$0")/.." && pwd)"
apk_dir="${APK_DIR:-$HOME/apks}"
sidecar="${COVE_SIDECAR:-$HOME/projects/cove/scripts/apk-sidecar.py}"
cd "$repo"

sha="$(git rev-parse --short=7 HEAD)"
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "alpha-apk: warning: uncommitted changes; the name still uses $sha" >&2
fi

out_dir="app/build/outputs/apk/$variant"
./gradlew "$task"

# Newest APK of the variant (app-debug.apk, app-beta.apk or app-beta-unsigned.apk).
built="$(ls -t "$out_dir"/*.apk 2>/dev/null | head -n1 || true)"
if [ -z "$built" ]; then
  echo "alpha-apk: no APK in $out_dir" >&2
  exit 1
fi

mkdir -p "$apk_dir"
dest="$apk_dir/KlipSilo-$sha-$suffix.apk"
cp -f "$built" "$dest.tmp"
mv -f "$dest.tmp" "$dest"
echo "alpha-apk: $dest"

if [ -f "$sidecar" ]; then
  python3 "$sidecar" --app klipsilo --apk "$dest" --repo "$repo"
else
  echo "alpha-apk: warning: $sidecar not found; no sidecar written" >&2
fi
