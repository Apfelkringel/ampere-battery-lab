# Verification record

Last verified: 2026-09-09 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.79` (`versionCode 79`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `bfa6d475cb5dfdeed766846c053b5c2e7961a7eb0a1dfb476ca9778f00514450`
- The same hash is published in the public update repository manifest.

## Runtime checks

| Runtime | Result |
| --- | --- |
| Android 14 / API 34 | App launch and update-path checks passed; foreground monitor remained active. |
| Android 16 / API 36 | App launch, foreground monitor and signed `0.78 -> 0.79` package update passed. `firstInstallTime` and preferences were preserved; no fatal exception occurred. |
| Android 17 / API 37 | Fresh signed `0.79` install, app launch, foreground monitor and navigation through Overview, Charge, Drain, Health and History passed; no fatal exception occurred. |

The API-37 system image and AVD are generated test assets stored under
`tooling/` on the external SSD and are ignored by Git.

## Persistence and backup

- `adb install -r` update testing preserved the app data directory and
  `firstInstallTime`.
- Android full-backup transport testing measured both included preference files
  and completed successfully with the production encrypted-transport rule.
- A visible JSON backup/restore test restored history, settings and telemetry
  after clearing the app data.

Android backup remains subject to the device's account, transport, encryption,
OEM and user settings. The visible export is the deterministic fallback before
uninstall/reinstall.

## Measurement limitations

Battery current, charge counter, design capacity and cycle count are exposed by
Android and/or the device vendor. A device can legitimately return unavailable
values; the app displays that state and uses documented local fallbacks. App
drain attribution is an estimate based on foreground usage and local battery
telemetry, not a privileged replacement for Android's internal battery stats.
