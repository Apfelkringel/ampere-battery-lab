#!/usr/bin/env bash
set -euo pipefail

name_file="store/google-play/listing/app-name.txt"
metadata_file="${1:-store/google-play/listing/short-description.txt}"
test -f "$name_file"
test -f "$metadata_file"

app_name="$(tr -d '\r\n' < "$name_file")"
name_characters="$(printf '%s' "$app_name" | wc -m | tr -d ' ')"
if (( name_characters == 0 || name_characters > 30 )); then
  echo "Google Play app name must contain 1-30 characters; got $name_characters in $name_file." >&2
  exit 1
fi

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

echo "Google Play metadata valid: app name $name_characters/30, short description $characters/80 characters."
