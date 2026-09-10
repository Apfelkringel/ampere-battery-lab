# Verification record

Last verified: 2026-09-10 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `1.00` (`versionCode 100`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `df57c490b3962c950e2cce20a5bbfa3f8221f4b70b422d6f8857b3740cd45253`
- The same hash is published in the public update repository manifest.

## Runtime checks

| Runtime | Result |
| --- | --- |
| Android 14 / API 34 | Fresh signed `0.85` install, compact German UI check, light/dark theme check, all five tabs, app launch and foreground monitor passed; no fatal exception occurred. The `0.87` responsive pass was additionally checked at 240 dp and 320 dp with the debug-equivalent build. |
| Android 16 / API 36 | Signed `0.91` artifact verified and compact 240/320/411-dp layouts plus a 700×1000 wide layout checked on the API-36 emulator; overview, charging, health, history and the wide-layout charge slider passed. App launch and foreground monitor passed. The release reports `versionCode 91`, `targetSdk 37`; no fatal exception occurred. |
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

Release `0.91` adds a history-load migration that removes legacy sessions with
zero percentage change. Release `0.93` applies the same rule when writing new
history entries, so a stale Android charge counter cannot create a misleading
`0 %` row; genuine charge energy remains in the aggregate total.

Release `0.92` adds a visible build label to the header. This makes it possible
to verify on the device that the newly installed APK, rather than a same-number
or stale-looking build, is running.

Release `0.93` refreshes the native dashboard palette, active navigation state,
live-status hierarchy and automatic-detection presentation. The local build
passed Android lint and the release workflow passed signing, package, version,
certificate and artifact checks.

Release `0.94` moves manifest lookup to the pinned GitHub Contents API path with
an explicit raw-content media type. This avoids depending on the staleable
`raw/main` manifest CDN while retaining the same fixed repository and APK
signature checks.

Release `0.95` measures text against the active centered content column, clips
the body to that column, adapts header controls below 390 dp, includes display
cutout insets, and uses a wider but bounded hero surface. The start page was
rendered on API 36 at 240×640, 320×640, 411×800, 700×1000 and 800×411.

Release `0.96` adds a dedicated wide-landscape composition for windows with at
least 600 dp width and less than 600 dp height. The live card and four key
metrics share the first viewport; the remaining chart stays in the same scroll
flow below them.

Release `0.97` adds a shorter-landscape composition for split-screen and small
tablet windows, keeping the live card inside the visible safe area. Insets now
reserve left and right cutout space as well as the system top and bottom bars.
The API-36 emulator was checked at 640×360, 600×411, 800×411 and 446×800
without unintended card or text overlap.

Release `0.98` aligns the header touch hitboxes with the two-button header used
below 390 dp. Settings and light/dark now respond at the exact visible button
positions on compact phone windows.

Release `0.99` synchronizes the light/dark system bars with the app theme,
improves light-theme contrast, and enlarges the header controls to consistent
48-dp touch surfaces while keeping their visual positions aligned.

Release `1.00` lets the settings dialog close by tapping outside it or using
the visible close button. Update checks, APK downloads and installer launches
now share an in-process lock and a persisted DownloadManager/installer guard,
so repeated taps and background/manual overlap cannot start parallel update
operations.

The current public update path was also exercised from signed `0.90` to signed
`0.91` on API 36. The app fetched the 0.91 manifest, showed the in-app dialog,
passed the cache-busted download and signature checks, completed Android's
installer flow, and reported `versionCode 91` afterward; `firstInstallTime`
remained unchanged.

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
