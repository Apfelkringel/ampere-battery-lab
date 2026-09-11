# Verification record

Last verified: 2026-09-11 (Europe/Berlin)

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.133` (`versionCode 133`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `4097a9e05425895b62fea9ce5f280e8d8eeae2c6026bd6201bee8b86f32a6233`
- The same hash is published in the public update repository manifest.

Release `0.133` ignores level reversals while charging when calculating the
local full-cycle fallback, preventing OEM recalibration from creating false
cycles. It retains the `0.132` validated OEM full-charge-capacity fallback.
Release `0.132` uses a validated OEM full-charge-capacity fallback for health
estimates and normalizes both µAh and mAh battery-driver units. It retains the
`0.131` cache-safe GitHub Contents API download.
Release `0.131` downloads the APK through the pinned GitHub Contents API as
raw bytes, avoiding a stale raw-CDN artifact after a manifest update. It keeps
the `0.130` measurable EFC label instead of the misleading "Ladeeffizienz"
percentage.
Release `0.130` replaces the previous misleading "Ladeeffizienz" percentage
with the measurable equivalent-full-cycle value (EFC). It keeps the
`0.129` extended Android health states and the `0.128` history cleanup.
Release `0.129` keeps the `0.128` history cleanup and additionally recognizes
the qualitative Android 16/17 health states exposed as values 8 and 9.
Release `0.128` removes zero-change and malformed legacy session rows even when
they use the old four-field format. It also displays Android's qualitative
battery-health signal separately from the locally estimated capacity percent;
the estimate remains bounded to 1–100% and is never derived from the
qualitative signal.

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

Release `0.100` lets the settings dialog close by tapping outside it or using
the visible close button. Update checks, APK downloads and installer launches
now share an in-process lock and a persisted DownloadManager/installer guard,
so repeated taps and background/manual overlap cannot start parallel update
operations.

Release `0.101` clips the colored card rails to the same rounded shape as their
cards and keeps the live mA value inside the status surface on compact layouts.

Release `0.102` centers every navigation button on its cell with symmetric
insets and centers icon/label groups on wide layouts as well.

Release `0.103` constrains dashboard labels, values and units to their owning
cards and columns, preventing long text from crossing borders on narrow,
wide and landscape layouts.

Release `0.104` centers horizontal navigation using a standard 24-dp icon box,
fixed icon/label spacing and the measured label as one shared layout block.

Release `0.105` includes that navigation layout in the published signed
artifact and scales the heart glyph to the same 24-dp icon language as the
other navigation symbols.

Release `0.106` uses the adaptive `600dp` window class for the overview,
keeping phone layouts consistent across density and device variations. The
wide overview separates the gauge from the status chip and status row.

Release `0.107` gives header actions one shared 48-dp control surface with
matching fill/outline bounds and centers the live badge as a single group.
The five navigation items use equal cell insets, a fixed 24-dp icon slot and
one shared label baseline on compact phones.

Release `0.108` selects the short landscape composition from the drawable
window height, keeping the live card above the gesture/navigation area on
Android 16/17 split-screen and landscape windows.

Release `0.109` bases that selection on the actual window orientation as well
as visible window bounds, because a scrollable dashboard reports its content
height rather than the height available above system navigation.

Release `0.110` reads the visible display frame for responsive breakpoints,
so the landscape layout remains correct when a scrollable dashboard is hosted
inside a resized Android 16/17 window.

Release `0.111` replaces the separate floating header action boxes with one
shared action rail and removes the redundant divider below the main navigation.
The action cells keep independent 48-dp touch regions while sharing one visual
container.

Release `0.112` keeps narrow charging values inside their owning columns,
rejects zero-change and wrong-direction sessions, and replaces web-prototype
demo values and placeholder pages with live browser battery data or explicit
unavailability states.

Release `0.113` makes the settings overlay close explicitly on outside-window
touch events as well as through its visible close button. The signed release
was rechecked on compact portrait and landscape windows after the change.

Release `0.114` handles devices whose dialog root consumes outside-window
touches by checking the visible dialog edge directly, so tapping the dimmed
area closes settings consistently on compact Android windows.

Release `0.115` removes the arbitrary 4,500-mAh design-capacity fallback. When
Android cannot expose the factory value, capacity-dependent calculations now
remain explicitly unavailable until a real value is detected or entered.

Release `0.116` persists the visible activity's active-session start state after
every battery read, so process restarts and backups do not lose the current
charging/discharging start level, timestamp or charge-counter baseline.

Release `0.117` expands nominal-capacity detection across battery/BMS driver
nodes, energy/voltage pairs and the Android PowerProfile, while retaining the
source label and strict 500–30,000 mAh bounds. Missing temperature, voltage and
capacity values remain unavailable instead of being rendered as `0` values.
The public APK was installed over `0.116` on API 36; `firstInstallTime` stayed
unchanged and the launch/logcat check reported no fatal exception.

Release `0.118` adds a bounded fallback hierarchy for Android-/BMS-reported
battery cycle counts. If neither source exists, the UI keeps the value
unavailable instead of displaying a misleading zero.

Release `0.119` scans additional OEM power-supply nodes and adds automated
tests for the shared charging-state and cycle-count validation rules.

Release `0.120` caps displayed battery health and its chart at 100 %, so an
optimistic benchmark can no longer render an impossible 110-% health value.

Release `0.126` makes the Quick Settings tile consume live battery broadcasts
while it is visible. Release `0.127` additionally requests a new TileService
binding from `MY_PACKAGE_REPLACED`. On API 36, an existing tile survived signed
updates `0.126` → `0.127` without removal/re-addition, reported live tile
accessibility text (`100% · Laden · +900mA · 25.0°C`), and the app's
`firstInstallTime` stayed unchanged.

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
values; the app displays that state and does not invent a device capacity.
Capacity-dependent projections remain unavailable until Android exposes a value
or the user enters the factory capacity. App drain attribution is an estimate
based on foreground usage and local battery telemetry, not a privileged
replacement for Android's internal battery stats.
