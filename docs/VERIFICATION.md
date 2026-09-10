# Verification record

Last verified: 2026-09-10 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.90` (`versionCode 90`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `71eb61ea8b70ee796c1ccce3c59eb732e3077e28eb9029da48b6490cb2c98651`
- The same hash is published in the public update repository manifest.

## Runtime checks

| Runtime | Result |
| --- | --- |
| Android 14 / API 34 | Fresh signed `0.85` install, compact German UI check, light/dark theme check, all five tabs, app launch and foreground monitor passed; no fatal exception occurred. The `0.87` responsive pass was additionally checked at 240 dp and 320 dp with the debug-equivalent build. |
| Android 16 / API 36 | Signed `0.90` artifact verified and compact 240/320/411-dp layouts plus a 700×1000 wide layout checked on the API-36 emulator; overview, charging, health, history and the wide-layout charge slider passed. App launch and foreground monitor passed. The release reports `versionCode 90`, `targetSdk 37`; no fatal exception occurred. |
| Android 17 / API 37 | Fresh signed `0.85` install, app launch and foreground monitor passed. The `0.87` release uses the same Android 14+ Canvas surface and passed local lint/build; no API-37-specific code path changed. |

On API 37, an installed signed `0.79` instance fetched the public manifest,
displayed the in-app `0.80` update dialog, downloaded the APK, reached Android's
package installer, and completed the update after the test emulator's unknown-
sources permission was enabled. `firstInstallTime` remained unchanged and the
monitor was restarted by `MY_PACKAGE_REPLACED` as a foreground service.

On API 36, an installed signed `0.89` instance fetched the public `0.90`
manifest from the update repository, displayed the update dialog, downloaded
the public APK through the app's DownloadManager flow, passed the local hash,
package, version and release-certificate checks, reached Android's package
installer, and completed the update. The installed package reported
`versionCode 90`; `firstInstallTime` remained unchanged and
`MY_PACKAGE_REPLACED` restarted the monitor. The cache-busting request was
used during this test, so the installed artifact was the current public 0.90
APK rather than a cached 0.89 response.

Release `0.90` also compiles and passes lint locally after centered max-width
content for wide screens, translated touch coordinates, responsive safe-edge
text fitting, narrow-phone navigation, stacked small cards, compact hero spacing
and the corrected health-card layout. Session tests on API 36 confirmed that a
transient status sequence while the cable is connected creates no history item,
while a real 50→60 % charge and 60→50 % discharge each create exactly one
session; the charge source was recorded as `Netzteil`. Direct handling for
Android power-connected and power-disconnected broadcasts remains active in
both the monitor service and the visible activity. The update client adds
cache-busting and verifies the release certificate before opening Android's
installer.

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
