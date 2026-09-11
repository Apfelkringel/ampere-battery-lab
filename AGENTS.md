# Ampere project completion rule

User-visible Ampere changes are not complete when source code is merely pushed.
Unless the user explicitly says otherwise, finish by incrementing the app version,
publishing the signed tagged release, updating the public APK and `latest.json` in
`ampere-battery-lab-updates`, and verifying the public APK version and SHA-256 hash.
