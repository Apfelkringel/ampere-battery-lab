#!/usr/bin/env bash
# Generate the Ampere Battery Lab release keystore from scratch. Only run
# this on a machine you fully trust; the resulting keystore is the private
# source of truth for all future APKs/AABs and signing-key rotation. Back
# up the resulting ~/.ampere directory to an encrypted vault.
set -euo pipefail

DEST="$HOME/.ampere"
mkdir -p "$DEST" && chmod 700 "$DEST"
KEYSTORE="$DEST/ampere-release.jks"
PROPS="$DEST/keystore.properties"

if [ -f "$KEYSTORE" ]; then
  echo "Keystore already exists at $KEYSTORE; refusing to overwrite." >&2
  exit 1
fi

STORE_PASS=$(openssl rand -base64 48 | tr -d '/+=' | head -c 48)
KEY_PASS=$(openssl rand -base64 48 | tr -d '/+=' | head -c 48)

keytool -genkeypair \
  -alias ampere-release \
  -keyalg RSA -keysize 4096 \
  -validity 9125 \
  -dname "CN=Ampere Battery Lab, O=Apfelkringel, C=DE" \
  -keystore "$KEYSTORE" \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -storetype JKS
chmod 600 "$KEYSTORE"

# PKCS12 mirror for tooling that prefers PKCS12 over JKS
PKCS12="$DEST/ampere-release.p12"
keytool -importkeystore \
  -srckeystore "$KEYSTORE" -srcstorepass "$STORE_PASS" -srcstoretype JKS \
  -srckeypass "$KEY_PASS" \
  -destkeystore "$PKCS12" -deststorepass "$STORE_PASS" -deststoretype PKCS12 \
  -srcalias ampere-release -destalias ampere-release
chmod 600 "$PKCS12"

cat > "$PROPS" <<EOF
# Ampere Battery Lab release signing credentials
# Generated $(date -u +%Y-%m-%dT%H:%M:%SZ)
# DO NOT COMMIT. Back this directory up to encrypted storage.
storeFile=$KEYSTORE
storePassword=$STORE_PASS
keyAlias=ampere-release
keyPassword=$KEY_PASS
EOF
chmod 600 "$PROPS"

# Symlink into the gradle module dir
ln -sfn "$PROPS" /Volumes/MacSSD/02_PROJECTS/Active/AccuBattery/android/app/keystore.properties

echo "Generated:"
echo "  $KEYSTORE"
echo "  $PKCS12"
echo "  $PROPS"
echo
echo "Next:"
echo "  bash tooling/release-ampere/secrets-dump.sh"
echo "  # paste outputs into GitHub repo Settings -> Secrets and variables -> Actions"
