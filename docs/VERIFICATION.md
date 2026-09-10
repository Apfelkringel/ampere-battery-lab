# Verification record

Last verified: 2026-09-10 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.86` (`versionCode 86`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `684c412be1e4b066297535dbcdab50b1f08d9e60efec9c26a468d23742260cb6`
- The same hash is published in the public update repository manifest.

## Runtime checks

| Runtime | Result |
| --- | --- |
| Android 14 / API 34 | Fresh signed `0.85` install, compact German UI check, light/dark theme check, all five tabs, app launch and foreground monitor passed; no fatal exception occurred. The `0.86` visual pass was additionally checked on the same compact layout with the debug-equivalent build. |
| Android 16 / API 36 | Signed `0.86` install, compact redesigned overview/charging UI, light theme, app launch and foreground monitor passed. The installed package reported `versionCode 86`, `targetSdk 37`; no fatal exception occurred. |
| Android 17 / API 37 | Fresh signed `0.85` install, app launch and foreground monitor passed. The `0.86` release uses the same Android 14+ Canvas surface and passed local lint/build; no API-37-specific code path changed. |

On API 37, an installed signed `0.79` instance fetched the public manifest,
displayed the in-app `0.80` update dialog, downloaded the APK, reached Android's
package installer, and completed the update after the test emulator's unknown-
sources permission was enabled. `firstInstallTime` remained unchanged and the
monitor was restarted by `MY_PACKAGE_REPLACED` as a foreground service.

Release `0.86` also compiles and passes lint locally after the complete visual
redesign, consistent card surfaces, chart scales, compact translated labels,
right-aligned long values and pressed-state feedback. Direct handling for Android
power-connected and power-disconnected broadcasts remains active in both the
monitor service and the visible activity.

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
