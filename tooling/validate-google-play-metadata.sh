#!/usr/bin/env bash
set -euo pipefail

root="store/google-play/listing"

read_value() {
  cat "$1"
}

character_count() {
  printf '%s' "$1" | wc -m | tr -d ' '
}

for locale in de en-US es-ES fr-FR it-IT pt-BR nl-NL; do
  if [[ "$locale" == "de" ]]; then
    directory="$root"
  else
    directory="$root/locales/$locale"
  fi

  name_file="$directory/app-name.txt"
  short_file="$directory/short-description.txt"
  full_file="$directory/full-description.txt"
  test -f "$name_file"
  test -f "$short_file"
  test -f "$full_file"

  app_name="$(read_value "$name_file")"
  name_characters="$(character_count "$app_name")"
  if (( name_characters == 0 || name_characters > 30 )); then
    echo "Google Play app name for $locale must contain 1-30 characters; got $name_characters in $name_file." >&2
    exit 1
  fi
  if [[ "$app_name" != AkkuTakt:* ]]; then
    echo "Google Play app name for $locale must keep the AkkuTakt brand and a localized search descriptor: $name_file." >&2
    exit 1
  fi

  short_value="$(read_value "$short_file")"
  short_characters="$(character_count "$short_value")"
  if (( short_characters == 0 || short_characters > 80 )); then
    echo "Google Play short description for $locale must contain 1-80 characters; got $short_characters in $short_file." >&2
    exit 1
  fi
  if [[ "$short_value" != *'—'* ]]; then
    echo "Google Play short description for $locale must use an em dash (—) in $short_file." >&2
    exit 1
  fi
  if [[ "$short_value" == *'–'* || "$short_value" == *'--'* ]]; then
    echo "Google Play short description for $locale contains a prohibited dash variant in $short_file." >&2
    exit 1
  fi

  full_value="$(read_value "$full_file")"
  full_characters="$(character_count "$full_value")"
  if (( full_characters == 0 || full_characters > 4000 )); then
    echo "Google Play full description for $locale must contain 1-4000 characters; got $full_characters in $full_file." >&2
    exit 1
  fi

  echo "Google Play $locale metadata valid: app name $name_characters/30, short description $short_characters/80, full description $full_characters/4000."
done

spanish_name="$(read_value "$root/locales/es-ES/app-name.txt")"
compatibility_name="$(read_value "$root/app-name-es-ES.txt")"
if [[ "$spanish_name" != "$compatibility_name" ]]; then
  echo "The es-ES compatibility file must match the localized Spanish title in locales/es-ES/app-name.txt." >&2
  exit 1
fi

echo "All seven localized Google Play listings and the es-ES compatibility source are valid."
