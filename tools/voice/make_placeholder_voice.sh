#!/usr/bin/env bash
# Generates PLACEHOLDER Nepali clips with macOS text-to-speech, for local testing only.
#
#   tools/voice/make_placeholder_voice.sh            # writes tools/voice/out/*.m4a
#   cp tools/voice/out/*.m4a app/src/main/assets/voice/ne/   # try them in a debug build
#
# Do NOT commit these files: system voices are not licensed for redistribution.
# Real clips must be recorded by a human volunteer (see tools/voice/README.md).
set -euo pipefail
cd "$(dirname "$0")"
VOICE="${VOICE:-Lekha}"   # hi_IN voice; reads Devanagari Nepali reasonably
mkdir -p out
tail -n +2 clips.csv | while IFS=, read -r clip text; do
  say -v "$VOICE" -o "out/$clip.aiff" "$text"
  afconvert -f m4af -d aac -c 1 "out/$clip.aiff" "out/$clip.m4a"
  rm "out/$clip.aiff"
done
echo "Wrote $(ls out/*.m4a | wc -l | tr -d ' ') placeholder clips to tools/voice/out/"
