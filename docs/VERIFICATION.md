# Verification record

Last verified: 2026-09-10 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.81` (`versionCode 81`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `aa11a3fb5a671182ffff822b572dbacabb8bb2b42cecbd62b01453aa79ca8442`
- The same hash is published in the public update repository manifest.

## Runtime checks

| Runtime | Result |
| --- | --- |
| Android 14 / API 34 | Fresh signed `0.81` install, app launch and foreground monitor passed; no fatal exception occurred. |
| Android 16 / API 36 | Signed `0.80 -> 0.81` replacement, app launch and foreground monitor passed. The installed package reported `versionCode 81`, `targetSdk 37`, preserved `firstInstallTime`; simulated disconnect/connect changed the UI between `On battery` and `Charging detected`; no fatal exception occurred. |
| Android 17 / API 37 | Fresh signed `0.81` install, app launch and foreground monitor passed; no fatal exception occurred. Earlier `0.80` navigation through Overview, Charge, Drain, Health and History also passed. |

On API 37, an installed signed `0.79` instance fetched the public manifest,
displayed the in-app `0.80` update dialog, downloaded the APK, reached Android's
package installer, and completed the update after the test emulator's unknown-
sources permission was enabled. `firstInstallTime` remained unchanged and the
monitor was restarted by `MY_PACKAGE_REPLACED` as a foreground service.

Release `0.81` also compiles and passes lint locally after adding direct handling
for Android power-connected and power-disconnected broadcasts in both the monitor
service and the visible activity.

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
