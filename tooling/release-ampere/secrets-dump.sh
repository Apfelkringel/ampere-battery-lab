#!/usr/bin/env bash
# Print values to paste into GitHub Actions secrets (Repository settings ->
# Secrets and variables -> Actions -> New repository secret).
#
# Only outputs to stdout; no file is written. Double-check that the only
# files involved are the locally generated ones in ~/.ampere/ and the
# Android SDK debug keystore at ~/.android/debug.keystore.
set -euo pipefail

NEW_KS="$HOME/.ampere/ampere-release.p12"
DEBUG_KS="$HOME/.android/debug.keystore"
PROPS="$HOME/.ampere/keystore.properties"

if [ ! -f "$NEW_KS" ]; then
  echo "Missing release keystore at $NEW_KS" >&2
  echo "Re-generate with tooling/release-ampere/bootstrap.sh" >&2
  exit 1
fi
if [ ! -f "$PROPS" ]; then
  echo "Missing credentials at $PROPS" >&2
  exit 1
fi

STORE_PASS=$(awk -F= '/^storePassword=/{print $2; exit}' "$PROPS")
KEY_PASS=$(awk -F= '/^keyPassword=/{print $2; exit}' "$PROPS")

echo "# Copy the line below into GitHub -> Settings -> Secrets -> Actions"
echo "# Secret name: AMPERE_ROTATED_KEYSTORE_BASE64"
echo "AMPERE_ROTATED_KEYSTORE_BASE64=$(base64 -i "$NEW_KS" | tr -d '\n')"
echo
echo "# Copy the line below into GitHub -> Settings -> Secrets -> Actions"
echo "# Secret name: AMPERE_KEYSTORE_BASE64 (Android debug keystore, used as V1 primary)"
echo "AMPERE_KEYSTORE_BASE64=$(base64 -i "$DEBUG_KS" | tr -d '\n')"
echo
echo "# Copy the line below into GitHub -> Settings -> Secrets -> Actions"
echo "# Secret name: AMPERE_KEYSTORE_PASSWORD"
echo "AMPERE_KEYSTORE_PASSWORD=$STORE_PASS"
echo
echo "# Copy the line below into GitHub -> Settings -> Secrets -> Actions"
echo "# Secret name: AMPERE_KEY_ALIAS"
echo "AMPERE_KEY_ALIAS=ampere-release"
echo
echo "# Copy the line below into GitHub -> Settings -> Secrets -> Actions"
echo "# Secret name: AMPERE_KEY_PASSWORD"
echo "AMPERE_KEY_PASSWORD=$KEY_PASS"
