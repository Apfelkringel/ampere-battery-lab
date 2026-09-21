# Ampere release-signing helper

The release signing keystore is the private source of truth for Ampere
APKs and AABs. The CI workflow build-apk.yml reads it from the following
GitHub Actions secrets (encoded as base64 because secrets must be flat
strings):

| Secret | Value |
|---|---|
| AMPERE_KEYSTORE_BASE64 | base64 of the debug keystore at ~/.android/debug.keystore used as the V1/JAR primary signer so 0.471-0.474 installations can update in place. The runner has the Android debug keystore by default; this secret overrides the path. |
| AMPERE_ROTATED_KEYSTORE_BASE64 | base64 of the rotated release keystore at ~/.ampere/ampere-release.p12 created 2026-09-21 with subject CN=Ampere Battery Lab, O=Apfelkringel, C=DE (RSA-4096, 25 years). |
| AMPERE_KEYSTORE_PASSWORD | the store password. Stored in ~/.ampere/keystore.properties. |
| AMPERE_KEY_ALIAS | ampere-release |
| AMPERE_KEY_PASSWORD | the key password for the ampere-release alias; same value as AMPERE_KEYSTORE_PASSWORD is fine. |

tooling/release-ampere/secrets-dump.sh prints the strings ready to paste
into the repository Actions secrets UI.

## Local build

The build no longer needs AMPERE_* env vars if a keystore.properties points
to a valid keystore. Symlink-friendly default:

    ln -s ~/.ampere/keystore.properties android/app/keystore.properties

gradle :app:assembleDirectRelease -x lintVitalAnalyzeDirectRelease then
signs with the release keystore. assemblePlayRelease builds the Play AAB.
The apksigner rotate step adds the debug keystore as V1 primary so existing
installations keep updating in place.

## Local rotation of the keystore

1. Generate a fresh keystore alongside the current one.
2. Re-run apksigner rotate --old-signer X --new-signer Y to extend the
   lineage file android/ampere-release.lineage.
3. Bump the EXPECTED_RELEASE_CERT_SHA256 pin in UpdateChecker.java to the
   new SHA-256.
4. Bump versionCode / versionName.
5. Publish per the AGENTS.md completion rule.
