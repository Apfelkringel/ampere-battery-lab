#!/usr/bin/env bash
set -euo pipefail

metadata_file="${1:-store/google-play/listing/short-description.txt}"
test -f "$metadata_file"

value="$(tr -d '\r\n' < "$metadata_file")"
characters="$(printf '%s' "$value" | wc -m | tr -d ' ')"

if (( characters == 0 || characters > 80 )); then
  echo "Google Play short description must contain 1-80 characters; got $characters in $metadata_file." >&2
  exit 1
fi

if [[ "$value" != *'—'* ]]; then
  echo "Google Play short description must use an em dash (—) in $metadata_file." >&2
  exit 1
fi

if [[ "$value" == *'–'* || "$value" == *'--'* ]]; then
  echo "Google Play short description contains a prohibited dash variant in $metadata_file." >&2
  exit 1
fi

echo "Google Play short description valid: $characters/80 characters."
