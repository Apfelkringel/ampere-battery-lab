# Security

Ampere Battery Lab is designed as a local-first battery monitor. Battery
telemetry, foreground-app names and backups are processed locally. If a user
opts in, the app sends allow-listed product-usage events and technical app/device
metadata to Google Analytics for Firebase. The app does not upload battery data
to an Ampere server.

## Release verification

Android release builds require the private release keystore in CI and refuse
debug or fallback signing. The direct APK updater verifies, before Android's
installer is opened:

1. HTTPS-only allow-listed GitHub URL
2. SHA-256 hash from the public release manifest
3. package name and version code
4. expected release certificate fingerprint

The GitHub release workflow also verifies the signer and publishes build
provenance attestations for the APK and Play App Bundle.

## Data protection

Network cleartext traffic is disabled by the Android manifest and network
security configuration. Android backup rules include only Ampere's two local
preference stores and cloud backup is disabled when the transport cannot
provide encryption capabilities. The app never requests contacts, location,
microphone, camera or an account login.

## Reporting a vulnerability

Please report security issues privately through GitHub's Security Advisories
for this repository. Include the affected version, device/Android version,
steps to reproduce and a minimal proof of concept. Do not publish sensitive
details before a fix is available.
