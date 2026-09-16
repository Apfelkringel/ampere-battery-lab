# Verification record

Last verified: 2026-09-16 (Europe/Berlin)

Version `0.388` makes background-monitor health understandable from Settings:
it reports when the monitor last persisted a heartbeat, flags missing/stale
signals after 30 minutes, and explains that opening Ampere restarts monitoring.
The copy deliberately describes a recent service heartbeat rather than
claiming Android can reliably report a background process as running. Tests
cover recent, missing, future (reboot/clock-domain mismatch), and stale
heartbeats. Direct/Play tests and lint pass locally; signed-release/public
artifact verification is pending.

Version `0.387` makes the app-usage summary distinguish missing battery
measurements from measured drain that could not be assigned to an app. Assigned
mAh are now explicitly labeled as estimates, and the remainder is disclosed as
potentially unattributed. The in-dialog back target is now 48 dp wide. Tests
cover summary wording and the minimum touch width. All 178 Direct and 178 Play
unit tests, both lint tasks and both debug APK builds pass.
The signed tagged release and internal Play draft upload succeeded. Public
APK/AAB SHA-256 values are `3539120943d6a7f7d294bf6310635d4fa6995813daacd78201ae9dfc5d94ae4a`
and `782824123378aec4c24bb4735881cd5230b15aafb78fb19feec0d30299e3a3db`;
downloaded public artifacts matched both values and the APK reports version
`0.387`.

Version `0.386` excludes future-dated telemetry from the seven-day local charge
and discharge rate windows. A shared inclusive time-window filter canonicalizes
valid rows and is covered for lower/upper boundaries, unsorted input, and future
samples. This matters because device wall time can jump after a user or network
clock adjustment; interval estimates must not consume samples beyond “now”.
The tagged build, signed public APK, public APK/AAB hashes and internal Google
Play draft upload all verified successfully. Public APK SHA-256:
`413f8999eefbd5c73091067d623681e36734e427e8628575dbfe4a19624f0897`.
Public AAB SHA-256:
`716eed51afed1086b7bcb00fa98cbd465a387fdcb27963601c1992cb6a17ebda`.

Version `0.385` improves the history balance chart legend: the four series now
use a two-row layout with clearer labels (“Verschleiß · EFC” and “Ladequote”)
so they remain readable on narrow phone screens. Direct and Play debug builds,
their unit tests and lint checks all pass. Gradle was run with JDK 17; the
system-default JDK 26 cannot process this Android SDK's `core-for-system-modules.jar`.

Public version `0.387` is verified in the update repository manifest and its
APK/AAB hashes match the published artifacts. The `0.384` change made the
history ratio honest about its meaning, kept a valid zero-percent value visible,
and aligned wide-screen period-button touch and accessibility targets. The
`0.385` legend improvement is recorded above. All 176 direct and 176 Play unit
tests pass, along
with both lint tasks and debug APK assembly for the version-bumped source. A
fresh emulator screenshot
check could not be completed: the existing AVD installation is signed with a
different certificate, so replacing it would erase its local app data.

## Current development change

Version `0.387` clarifies that per-app mAh are estimates allocated from
device-level telemetry and foreground usage, not exact app energy measurements.
The summary now differentiates missing device readings from readings that
cannot be attributed to an app. The per-app details back button now meets the
48-dp minimum touch width.

Version `0.386` prevents restored or clock-shifted telemetry with timestamps in
the future from influencing live charge/discharge forecasts. Both calculations
now consume validated samples within the same seven-day window ending at the
current wall-clock time; tests cover bounds, chronological ordering and future
sample exclusion.

Version `0.385` keeps all four history-chart series legible at compact widths by
placing their color keys in two rows and replacing abbreviated “EFC” and
“Lade/Verbrauch” legend text with “Verschleiß · EFC” and “Ladequote”. The plot
retains its date labels and the individual per-series scaling note.

Version `0.384` replaces the misleading history-chart label “Effizienz” with
“Geladen/Verbrauch” and explains that this is the ratio of charged to consumed
energy, not a measured cell efficiency. A measured 0% ratio is no longer
presented as missing data. History-period tap targets now use the centered
content coordinates on wide displays, share their six-dp gutters consistently,
and match the TalkBack focus bounds. Regression tests cover zero-valued ratios
and wide-screen target geometry.

## Earlier development changes

Version `0.282` additionally follows the supplied reference interaction
language instead of only recoloring the existing UI: selected navigation and
enabled toggles are filled turquoise actions, neutral controls remain quiet,
the background is flat, and a compact top color rail replaces the previous
glossy/green emphasis. The existing battery data hierarchy and semantic amber
temperature / blue discharge cues remain intact.

Version `0.281` additionally applies the supplied visual reference language to
the Android Canvas UI: the primary signal color is now a cool blue-green,
surfaces and borders move to a blue-black instrument palette, and selected
navigation/actions use the same restrained turquoise cue. Temperature remains
amber as a semantic exception; discharge remains blue so direction is still
readable without relying on color alone.

Version `0.281` additionally preserves the full right-hand charging result on
the same 240-dp layout: the compact current lane ends before the time column,
while the result column starts early enough that `Erreicht` remains readable.

Version `0.280` additionally gives the charging header a dedicated compact
layout below 270 dp. The current value keeps a measured text lane and a
separate gutter before the time column, so a real value such as `900 mA` is
not rendered as a clipped `90…` on narrow phones.

Version `0.279` additionally removes the single-sample current scale
heuristic. Android current properties and battery power-supply nodes are
converted from their documented microampere representation and validated for
sentinels, zero and implausible bounds; a plausible low current is no longer
multiplied merely because it falls below an assumed typical charge or drain
rate. This prevents real idle drain from being reported as a fabricated 10x,
100x or 1000x value.

Version `0.278` additionally makes the current known Android battery status
authoritative in the dashboard; a persisted monitor sample is used only when
Android reports an unknown status, so a fresh unplug event cannot be masked for
up to two hours. Version `0.277` additionally removes the last isolated purple hardcodes from
secondary telemetry surfaces and uses one restrained eucalyptus tone throughout
the dashboard. Version `0.276` additionally removes the isolated purple screen-time accent
from the dashboard and uses a restrained eucalyptus tone that belongs to the
existing green-neutral visual system. Version `0.275` additionally keeps the charge-baseline anchor powered when
Version `0.275` additionally keeps the charge-baseline anchor powered when
an OEM omits `EXTRA_PLUGGED` but the resolved battery state is charging/full;
this prevents a valid charging session from being treated as an unplug point.
Version `0.274` additionally marks virtual Canvas controls visible to
accessibility services only while their real screen bounds intersect the
visible dashboard, preventing clipped offscreen nodes from being announced as
visible at `[0,0]`. Version `0.273` additionally gives successful Canvas control activations a
single system-respecting haptic confirmation and cancels header actions when
the finger is released over a different control. Version `0.272`
additionally shares the direction-aware current label with the
persistent monitor notification, so every live output surface keeps the same
charging/discharging meaning. Version `0.271` additionally shares
direction-aware current and power labels across the widget, Quick Settings tile, overlay and charging screensaver, so
discharge is never presented as an unsigned charge value. Missing telemetry is
rendered as unavailable instead of a fabricated zero. Version `0.270`
additionally preserves Android's charging status when an OEM
omits `EXTRA_PLUGGED`, while still treating an explicit zero as unplugged.
Version `0.269` additionally keeps local charge-time profiles separate for
AC, wireless, USB and dock sources and does not carry an interval across a
charger change. Version `0.268` additionally caps monitoring and screen-time integration after
a service outage and prevents the separately measured suspend time from being
counted a second time. Version `0.267` additionally reserves already attributed direct app drain
before distributing the remaining observed energy to apps without a direct
telemetry sample, so displayed app estimates cannot add up beyond the observed
discharge. Version `0.266` additionally preserves Android activity class names across the
legacy `MOVE_TO_BACKGROUND`/modern `ACTIVITY_PAUSED` event-number collision, so
in-app activity switches are not closed early. Version `0.265` additionally keeps app-drain attribution bounded when no
observed discharge exists, avoids replacing short exact usage sessions with an
unbounded daily bucket, and caps stale telemetry attribution at the shared
accounting interval. Regression tests cover all three guards. Version `0.264`
additionally prevents the OEM current-scale heuristic from
inflating plausible sub-10-mA deep-sleep readings; the regression is covered
by a unit test. Version `0.263` additionally refines the smooth button system with an
Energy Rail visual language, tonal depth, readable active-tab contrast and
responsive text bounds. Version `0.262` additionally aligned the charge
controls' touch/accessibility geometry without overlap. Version `0.261`
additionally derives battery-side power from the existing
validated current and voltage telemetry. Charging and discharging ranges are
kept separate, and minimum/average/maximum values are included in the
diagnostic Research JSON and as a derived `battery_power_mw` CSV column.
The old 11-column stored telemetry format remains untouched, so restored data
from older releases stays readable.
Version `0.260` additionally reads Android's optional
`BATTERY_PROPERTY_ENERGY_COUNTER` as remaining battery energy in nWh and Wh.
The value is validated against a conservative physical range, displayed in the
live dashboard and monitor notification, and exported separately from the
charge counter; unsupported or implausible device values remain unavailable.
Version `0.259` additionally computes minimum, average and maximum from the
same validated temperature telemetry used by the diagnostic report. The
statistics are shown together in the health view, history diagnosis and
Research JSON; missing/zero values are excluded. Version `0.258` additionally
reads the optional read-only Linux/OEM
`manufacture_year`, `manufacture_month` and `manufacture_day` fields. Only a
complete valid Gregorian date is accepted, then shown in the health view and
Research JSON; Samsung-specific `LLB MAN` log parsing and privileged access
remain intentionally outside the universal no-root path. Version `0.257`
additionally preserves and exports both optional read-only
Linux/OEM `charge_control_start_threshold` and `charge_control_end_threshold`
values. When both are valid, the UI reports the actual OEM charging window
as start–end percentage instead of silently discarding the lower threshold.
The OEM window remains separate from the app's own target and no sysfs write
is performed. Version `0.256` additionally reads the optional read-only Linux/OEM
`charge_behaviour` attribute. The four ABI states (`auto`, `inhibit-charge`,
`inhibit-charge-awake`, `force-discharge`) are normalized exactly, including
the bracketed active-value format used by some sysfs drivers. The dashboard,
notification and Research JSON explain the active charging behaviour without
writing to sysfs or conflating it with the separate charging algorithm.
Version `0.255` additionally reports minimum, average and maximum current for
the visible, direction-filtered telemetry window. Zero, negative and missing
values are excluded before aggregation; the statistic is computed by a pure
tested helper and the chart no longer hides the minimum. Version `0.254` additionally reads the optional read-only Linux/OEM
`capacity_error_margin` value. Zero through 100 percent are accepted, including
zero after a calibration; the value is presented as Fuel-Gauge-
Messunsicherheit and never as battery wear or health. It is shown alongside
the dynamic ESR diagnostic and included in Research JSON. Version `0.253` additionally reads the optional read-only Linux/OEM
`charge_type`/`charge_types` value and keeps it separate from Android's
qualitative `EXTRA_CHARGING_STATUS`. Only the ABI's known algorithms are
accepted; the active bracketed value from `charge_types` is supported as a
fallback. The dashboard, notification and Research JSON label it as the
kernel charging algorithm. Version `0.252` additionally reads the optional read-only Linux/OEM
`internal_resistance` value in µΩ from ranked battery/BMS supplies and shows
it as a dynamic ESR diagnostic in the health view and Research JSON. The value
is range-checked, briefly cached and explicitly not converted into a health
percentage because it varies with state of charge and temperature. Version
`0.251` additionally falls back to the read-only Linux/OEM
`constant_charge_current_max` node when Android exposes no charger capability
pair. This value is labeled as a hardware charging ceiling, never as the
instantaneous battery current or the external adapter power; the reader ranks
battery/BMS supplies, caches only a readable path briefly and rejects invalid
units and sentinels. Version `0.250` additionally keeps Android's optional charger capability pair
as separate validated maximum current, maximum voltage and derived power values.
The UI, monitor notification and Research JSON label these as the charger
profile, never as the instantaneous battery current. Missing half-pairs remain
partially visible only when the individual value is plausible; invalid units,
sentinels and implausible ranges stay unavailable.
Version `0.249` additionally validates and displays the public Android battery
technology string (for example `Li-ion`) in the health view and Research JSON;
control characters and oversized OEM strings remain unavailable. Version
`0.248` additionally exports a human-readable local TXT diagnostic
report from the same validated telemetry summary, alongside the existing CSV
and Research-JSON choices. Version `0.247` additionally adds a conservative local telemetry diagnosis:
maximum battery temperature, peak discharge current, minimum discharge voltage
and sampling gaps are calculated from validated chronological rows. The
research JSON carries the same structured summary; no fixed 2S-pack voltage
threshold and no live "early cutoff" claim are used. This keeps the
BatteryLog-inspired report useful on ordinary 1S phones as well as multi-cell
devices. Version `0.246` additionally reads an optional read-only OEM charge end
threshold from battery/BMS power-supply nodes and displays it separately from
Ampere's user alarm target. It never writes charge-control files. Version
`0.245` additionally surfaces Android's public device-wide thermal
status separately from the battery sensor temperature in the charging view and
monitor notification. Version `0.244` additionally blends a sufficiently observed current discharge
phase with the local seven-day history. The current phase reaches at most 60%
weight after 30 minutes; charge-counter energy can support the rate when the
displayed percentage is flat. Observed charging and discharging intervals are
also capped, so a stopped monitor cannot inflate the session duration.
Version `0.243` additionally uses optional read-only Fuel-Gauge estimates for
time-to-full and time-to-empty when Android's system prediction is unavailable.
The `now` value is preferred over the averaged full-charge value, Linux
sentinels and values beyond 48 hours are rejected, and the fallback remains
available on older supported Android versions. Version `0.242` additionally rejects non-finite and physically implausible
local charge rates while preserving a valid historical rate when the current
sample is an outlier. Version `0.241` additionally bounds local time-to-full
and time-to-target
estimates: unknown capacity, non-finite rates and insufficient current now
remain unavailable instead of producing a synthetic one-minute result.
Version `0.240` additionally distinguishes a missing OEM `EXTRA_PLUGGED` field
from an explicit `EXTRA_PLUGGED=0`, so a status-first `CHARGING`/`FULL` reading
survives incomplete broadcasts without weakening explicit unplug detection.
Version `0.239` additionally centralizes the validated battery snapshot used by
the widget, Quick-Settings tile, overlay and Dream/screensaver. Version `0.238`
additionally validates battery voltage through a read-only
`voltage_now` fallback when Android's broadcast value is missing or implausible.
Battery and BMS nodes are ranked before other supported fuel-gauge names, both
mV and µV are normalized, and the working path is cached until it stops
producing a plausible value. Version `0.237` additionally makes foreground
usage aggregation activity-aware.
It keeps a set of active Activity classes per package, ignores duplicate
PAUSED/STOPPED closes, preserves an in-app Activity switch, and force-closes
all open sessions on Android's `SCREEN_NON_INTERACTIVE` event. Version `0.236`
additionally adds a local background watchdog. The foreground
service records a 10-minute elapsed-realtime heartbeat, while an
`AlarmManager.setAndAllowWhileIdle` check runs every 20 minutes and restarts
the service only after 30 minutes without a heartbeat. Rewound or missing
elapsed times are treated as stale. Version `0.235` additionally makes the
charge-target alarm edge-triggered and
persisted: it fires on an upward crossing, resets only after unplugging or a
3-percent drop below the target, and keeps the last level in backup data.
This follows the stateful target evaluator pattern in Battery Monitor while
retaining Ampere's first-valid-sample behavior. Version `0.234` additionally
uses Android's unrounded `level / scale` fraction
for the charge-counter full-capacity fallback, avoiding a needless whole-percent
rounding step while retaining the 20–100% and 500–30,000 mAh bounds. Version
`0.232` additionally applies a conservative OEM current-scale detector
to Android and battery-supply current readings. It only considers factors
1/10/100/1000, uses separate typical charging/discharging thresholds, and
does not amplify a low current during charging taper at 90% or above. Version
`0.231` additionally bounds per-app drain attribution by the observed
discharge energy. Direct local telemetry is scaled down when its sum would
exceed that energy; apps without direct telemetry use a time-weighted estimate,
and the UI labels both paths as estimates. This follows the same measurement
honesty boundary used by OpenMonitor and Device Watch. Version `0.230`
additionally adds an Android charge-counter fallback for
full-charge capacity when no driver/OEM value is exposed. It only estimates
from a validated 20–100% level and marks the provenance explicitly. Version
`0.229` additionally adds a persisted hybrid charge anchor: a new
full-charge anchor is created once per plug session, while unplugging below
99% creates an explicit unplugged baseline. This prevents later charge cycles
from being merged into an old "since full" statistic. Version `0.228` added a
monotone 90-day daily full-cycle history, exported in CSV and Research JSON,
plus the Research JSON now also carries the validated since-charge anchor
state. Version `0.227` added the system-selectable charging Dream/screensaver inspired
by the Apache-2.0 Dock project, plus the local low-battery notification from
version `0.226` inspired by the
configurable battery alarms documented by the open-source Battery Monitor
project. The daily history follows the same daily upsert/monotonicity idea as
PlusPlusBattery, but uses an own compact SharedPreferences format and tested
source precedence. The implementation is original and uses a 3-percent reset
hysteresis; the Dream uses only existing Android battery signals and adds no
runtime permission. Unit tests cover the cycle-history retention, source
precedence, threshold normalization, charging suppression and one-shot behavior.

## Release artifact

- Package: `com.ampere.batterylab`
- Version: `0.225` (`versionCode 225`)
- `minSdk 23`, `targetSdk 37`
- Release APK SHA-256: `a5b1163b20a7920d42de2f7395da4a0eb308a88660cfd146645505dcc0340ce8`

Release `0.225` additionally exposes the charging target as an Android
Accessibility `SeekBar` with the safe 50–100 percent range. TalkBack and
other services can set the value through `ACTION_SET_PROGRESS`; the normal
touch slider and the accessibility action share the same persistence and
alarm-cancellation path.

Release `0.224` additionally exposes the visible page controls through the
same virtual accessibility tree as the header: chart range, charging toggles,
benchmark, capacity editing, usage details and CSV export use page-local
screen bounds and stateful labels. Touch exploration also emits only one
hover-enter event per control.

Release `0.223` additionally supplies screen-space bounds for the Canvas
dashboard's virtual accessibility controls, so TalkBack can place focus on
the actual buttons even while the page is scrolled.

Release `0.222` additionally exposes the Canvas dashboard's header actions and
five navigation tabs as individually focusable Android accessibility controls,
including touch-exploration hover events. The visual layout and normal touch
geometry stay unchanged.

Release `0.221` additionally supplies Android 12+ with responsive widget
`RemoteViews` cutoffs for short, compact and standard sizes. Older launchers
keep the measured-size fallback; all layouts use the same validated battery
snapshot.

Release `0.220` additionally keeps health provenance atomic: invalid readings
cannot retain an old source label, and Samsung ASOC is named explicitly in
the UI instead of being reduced to a generic driver label.

Release `0.219` additionally accepts Samsung's explicit read-only `fg_asoc`
attribute as a validated ASOC health fallback while excluding the qualitative
`health` attribute. Values remain limited to 1–100 percent.

Release `0.218` additionally selects a dedicated one-row widget layout for
very low-height portrait or landscape placements. Compact and standard layouts
now use both the actual widget width and height, so details cannot be clipped
inside a resized widget.

Release `0.217` additionally resets an old open session after a long
unobserved sampling gap. The current point becomes a fresh baseline instead
of claiming the whole gap as continuously measured battery activity.

Release `0.213` additionally removes the platform-dependent emoji from the
optional live overlay. Its first line now uses stable text and measurements so
font metrics cannot change the overlay geometry across Android devices.

Release `0.212` additionally makes the visible LIVE control functional: it
refreshes the current Android battery broadcast immediately and keeps the
responsive header hitbox in one shared layout rule.

Release `0.211` additionally uses a compact, complete duration format in
metric cards. Values such as 20 hours and 4 minutes render as `20 h 4 m`
instead of being ellipsized inside narrow responsive cards.

Release `0.210` additionally reconciles a successfully completed persisted
DownloadManager update when the app becomes visible again. This recovers
from process reclamation between download completion and APK verification,
without starting a second download.

Release `0.209` additionally resolves the Android/OEM state-of-health
percentage and its source as one validated reading. An invalid system value
such as 110% can therefore neither reach the UI nor retain the wrong
"Android BatteryManager" label while a fallback is displayed.

Release `0.208` additionally refreshes one shared validated health snapshot
after stored-data reloads, live samples, restores and design-capacity changes.
All health surfaces therefore use the same source and cannot mix an invalid
system value such as 110% with a different capacity fallback.

Release `0.207` additionally resolves health percentage and its derived
capacity through one validated source snapshot. An invalid system SoH such as
110% is rejected before either value reaches the UI; the fallback percentage,
capacity and source remain consistent.

Release `0.206` additionally selects a compact non-overlapping home-screen
widget layout below 220 dp and re-renders each widget on resize using its
actual minimum width.

Release `0.205` additionally centralizes Linux power-supply source ranking
for current, capacity and cycle-count readers. A declared USB/input type is
rejected even when its directory name contains `battery`; battery, BMS and
fuel-gauge sources remain deterministically prioritized.

Release `0.204` additionally gives the top settings, theme and live controls
independent equal-size 48-dp surfaces with matching touch geometry. Narrow
phone layouts use two controls; wider layouts use three without visual or
label overlap.

Release `0.203` additionally checks the documented read-only OPlus/ColorOS
`battery_fcc` and `battery_soh` paths after the standard power-supply
hierarchy. Missing, inaccessible or implausible OEM values remain unavailable.

Release `0.202` additionally treats a Linux power-supply `cycle_count` of
zero as unavailable while preserving zero as a valid Android-reported count.
The scan also recognizes fuel-gauge node names used by additional OEMs.

Release `0.201` additionally applies one shared sampling-gap rule to the
level and current charts. Missed monitoring windows remain visible as gaps
instead of being rendered as invented transitions.

Release `0.200` additionally persists the critical DownloadManager metadata
and installer state synchronously before the app can be reclaimed. A completed
background download therefore remains verifiable after process recreation.

Release `0.199` additionally removes the legacy 110% headroom from the
capacity-measurement chart. The chart now uses the same 0–100% scale as the
health value, progress bar and all other health surfaces.

Release `0.198` additionally routes every displayed health value through one
final validation gate. Only 1–100 % can be shown; invalid OEM/API values and
legacy values such as 110 % become unavailable instead of being rendered.

Release `0.197` additionally plots charging/discharging current using the
actual telemetry timestamps. Long collection gaps are left as visual gaps
instead of being rendered as a fabricated continuous ramp.

Release `0.196` additionally adds a battery/BMS sysfs fallback for live
`current_now`/`current_avg` when both Android current properties are absent or
invalid. USB input supplies are excluded so battery-side power and sessions
cannot be based on wall-input current.

Release `0.195` additionally adds a read-only `state_of_health` fallback for
Android 14–17 OEM battery/BMS nodes. Only explicit values from 1–100 are
accepted; qualitative `health` files and impossible values such as 110 remain
unavailable.

Release `0.194` additionally reopens foreground and background sampling after
a wall-clock rollback. The next sample becomes the new cadence baseline while
chronological sorting keeps the preserved telemetry usable.

Release `0.193` additionally sorts valid telemetry rows by timestamp before
analytics, export and persistence. It also refuses to write a backwards
session after a wall-clock rollback, preserving the existing local data.

Release `0.192` additionally removes the unnecessary API-36 gate from the
Android BatteryManager state-of-health probe. Android 14/15 devices and OEM
backports may expose the feature-flagged property earlier; absent, blocked or
impossible values still fall back safely and can never render above 100 %.

Release `0.181` additionally keeps a valid Android state-of-health reading as
the shared source for both the displayed health percentage and derived mAh
capacity. Invalid benchmark preferences are no longer presented as a source.

Release `0.182` additionally records automatic health samples only after a
near-full charge (at least 95 %) with a measured stable charging current of at
most 25 mA; partial or still unstable charges are kept out of the health
estimate.

Release `0.183` additionally uses the latest valid charging-current sample at
the session boundary instead of the lowest current seen anywhere in the
session, so an early transient cannot qualify a later unstable charge.

Release `0.184` additionally expires power-broadcast edge hints after the
short synchronization window, so a missing follow-up broadcast cannot leave a
charging or discharging session permanently misclassified.

Release `0.185` additionally applies the same validated telemetry-row filter
to CSV and JSON export, dropping malformed, impossible or directionally
contradictory legacy rows in both formats.

Release `0.186` additionally applies that validated telemetry source to live
charge/discharge rates, app-attributed drain, current charts and timestamped
level charts. The background recorder also removes malformed legacy telemetry
when it next persists a sample, so invalid rows cannot contaminate analytics.

Release `0.187` additionally rejects short single-percent cable/status blips
without an energy reading; a real single-percent session remains recordable
when it has measured energy or lasts at least five minutes.

Release `0.188` additionally normalizes restored and persisted charge targets
to 50–100 % before they reach the dashboard, progress bar or charge alarm.

Release `0.189` additionally applies the health-percentage output gate to
every dashboard and service path, rejecting impossible direct values such as
`110 %`. Backup restore and telemetry migration now normalize persisted
telemetry immediately instead of waiting for a later background sample.

Release `0.190` additionally gives the wide-landscape live card extra bottom
padding, keeping its status row and bolt glyph visually separated from the
rounded outline.

Release `0.191` additionally canonicalizes stored level-history and session
rows during app startup and before background session append, so invalid
legacy rows are removed from the local data source rather than only hidden.

Release `0.180` additionally uses a robust median over the newest five valid
local capacity samples, so one noisy charge cannot dominate the health result.

Release `0.179` additionally rejects telemetry rows whose current direction
contradicts the stored charging flag before CSV/JSON export.

Release `0.178` additionally validates the written duration format and the
ordering of extended session timestamps before they reach details, exports or
the wear chart. Invalid legacy rows are removed while supported older rows stay
supported.

Release `0.177` additionally validates extended session levels, energy,
screen values and duration fields before they reach details, exports or the
wear chart; invalid legacy rows are removed while four-field legacy rows stay
supported.

Release `0.176` additionally rejects invalid EFC values in extended session
rows, keeping `NaN`, infinity, negative and oversized values out of the
wear chart while preserving older valid four-field rows.

Release `0.175` additionally rejects corrupt persisted EFC fractions; only a
finite remainder in `[0, 1)` is accepted, so corrupt values cannot create
false full cycles or leak `NaN` into the UI.

Release `0.174` additionally rejects invalid persisted phase percentages
(including 110 %, NaN and infinity) in calculations and UI fallbacks; only
real cumulative consumption since a full charge may exceed 100%.

Release `0.173` additionally keeps the Entladen view empty until a real
completed discharge exists; the current level is not shown as fake history.

Release `0.172` additionally removes the unused battery-optimization exemption
permission; monitoring, backup and the signed in-app updater do not require it.

Release `0.171` additionally keeps every initial status label consistent with
the unavailable state until a valid Android battery level arrives.

Release `0.170` additionally keeps the initial dashboard state unavailable
until a valid Android battery level arrives; no transient 0-% value is saved.

Release `0.169` additionally rejects impossible legacy percentage points in
stored history and chart telemetry instead of clipping them to 100%.

Release `0.168` additionally keeps malformed telemetry and session metadata
from aborting the structured research export; impossible rows are skipped.

Release `0.167` additionally uses a complete short "Kapazität" label on
phone-width overview cards instead of truncating "Geschätzte Kapazität".

Release `0.166` additionally keeps the wide-landscape gauge below the heading
and above the status row with explicit spacing.

Release `0.165` additionally keeps wide-landscape gauge and status elements in
separate geometry lanes so the status cannot be painted underneath the ring.

Release `0.164` additionally validates Android's raw level/scale pair in every
surface and in background session accounting; impossible OEM values remain
unavailable instead of becoming a fabricated percentage.

Release `0.159` additionally labels the actual health-data source on the
capacity card, so Android system SoH is not mislabeled as a local measurement.
Release `0.160` additionally validates battery temperature consistently across
the dashboard, service, widget, overlay, Quick-Settings tile and alarm.
Release `0.161` additionally validates battery voltage consistently before
display and battery-side power calculation.
Release `0.162` additionally validates the charge counter consistently in the
foreground and background paths before session and EFC calculations.
Release `0.163` additionally rejects impossible session changes above 100% and
re-derives noisy live transitions from valid measured energy where possible.
Release `0.158` rejects impossible Android state-of-health
values above 100% instead of silently converting them to 100%; local health
measurements remain the fallback.

Release `0.157` additionally validates Android current-property sentinels and
implausible spikes consistently across the dashboard, service, widget, overlay
and Quick-Settings tile.
Release `0.156` additionally keeps the temperature threshold choices visible
in the Android settings dialog.
Release `0.155` additionally adds a configurable temperature warning with
three-degree hysteresis and one notification per heat phase.
Release `0.154` additionally keeps the compact charging power label readable
on narrow 320/360 dp layouts without crossing its metric column.
Release `0.153` additionally calculates and displays validated battery-side
power separately from charger maximum power.
Release `0.152` additionally validates Android's optional charger current/
voltage pair and displays the resulting maximum source power separately from
the live battery current.
Release `0.149` additionally reads Android 16+'s qualitative capacity-level
signal separately from state-of-health, and exposes it only when the system
provides it.
Release `0.151` additionally displays Android's optional charging profile
signal separately from cable detection and session direction.
The next runtime-estimate change keeps local seven-day history first and uses
Android's bounded discharge prediction only as a labeled fallback.
Release `0.148` additionally resolves noisy charging/discharging level
snapshots against measured energy before writing a session row. This preserves
real charging sessions at OEM charge limits without creating zero-percent
history entries.
Release `0.147` filters legacy health-capacity samples once at
load time and writes the normalized list back, so invalid points cannot remain
in the health graph or future averages.
Release `0.146` additionally refreshes dynamic OEM full-charge-capacity values
every 15 minutes while the monitor is running, so learned fuel-gauge values do
not remain stale for the lifetime of the process.
Release `0.144` uses Android's runtime-available state-of-health property on
Android 16/17, with local-capacity fallback on unsupported or restricted
devices.
Release `0.143` uses one shared measured-capacity hierarchy for dashboard,
health page and background notification, including filtered recent samples.
Release `0.142` rejects implausible health-capacity samples before averaging,
uses overflow-safe accumulation, and keeps health at a maximum of 100 percent.
Release `0.141` bases landscape selection on the visible ScrollView viewport,
not the taller scroll content canvas. This keeps rotation and split-screen
layouts on the compact composition reliably.
Release `0.140` separates the compact landscape hero into clean horizontal
lanes so its status, gauge and health line cannot overlap.
Release `0.139` uses the actual Canvas dimensions for landscape selection on
Android rotation and keeps the compact overview card inside the safe band.
Release `0.138` makes the compact landscape overview height-aware. The live
card, gauge and health details stay inside the visible safe band above system
navigation, including short split-screen windows.
Release `0.137` moves monitor broadcast handling, sampling and local
persistence to a dedicated background thread, keeping the UI main thread
independent of telemetry work.
Release `0.136` keeps the visible dashboard aligned with the monitor's recent
stabilized charging state, so transient Android broadcasts cannot make the UI
and session logic disagree.
Release `0.135` prefers exact foreground/background usage events for app-drain
analysis; aggregated daily usage is retained as a compatibility fallback.
The signed `0.135` artifact was installed over the existing API-36 test
installation from the public Contents endpoint; the install timestamp stayed
unchanged. With usage access enabled, the Entladen view produced a foreground
app row from runtime usage events without a fatal exception.
Release `0.134` adds a conservative local equivalent-full-cycle estimate from
Android's persistent charge counter for devices without a system/BMS cycle
counter. Only stable counter increases while charging count; resets and
unplausible jumps are ignored, and the UI marks the estimate with `~`.
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
