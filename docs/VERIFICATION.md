# Verification record

Version `0.468` adds three more regression suites for the duration,
fuel-gauge and time-to-target helpers, all of which were previously
covered only by the runtime. `BatteryDurationTest` pins the compact and
dashboard minute/hour formatting including whole-hour, hour+minute and
the non-positive fallback to the em-dash. `BatteryFuelGaugeTimeTest`
pins the kernel-fuel-gauge normalizer including the 48-hour sentinel,
truncation-to-minute behaviour, the instantaneous-vs-average preference
and the all-invalid fallback to 0. `BatteryTimeEstimateTest` pins the
bounded minutes-to-target calculation including the 50 mA minimum,
100 000 mA maximum, non-finite historical rate, integer overflow and
the trivial-missing capacity clamping to at least one minute. The full
Direct-Debug unit-test suite (now 363 tests), build and lint passed
locally. CI release workflow
[`35410532448`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35410532448)
passed and the signed tag is `v0.468`. Public update-repository commit
[`7e7e21c`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/7e7e21c)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`6ada4ecfaff4dead3b2cc86a61232518cebc4df157530f710eb7f1eb210a2f86` and
reports package `com.ampere.batterylab`, version code/name `468`/`0.468`.
The Play internal-track publishing workflow
[`35410822711`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35410822711)
reported a billing-related GitHub-side failure that has to be retried
once the account is restored; the signed APK and AAB are byte-identical
with `latest.json` and are ready for upload.

Last verified: 2026-09-19 (Europe/Berlin)

# Verification record

Version `0.467` finishes the App-Verbrauchs perf pass and adds two more
regression suites. The App-Verbrauchs dialog now routes its launcher icon
through `AppLabelCache.iconFor` so the previously cached drawables from the
canvas variant are reused instead of triggering a fresh `getApplicationIcon`
lookup per row. `UsageEventAccumulatorTest` pins the activity-aware
foreground/background accounting — single-class close, MOVE_TO_BACKGROUND
without a class name, multi-activity overlap, duplicate close events, the
window-end early-return filter and independent package sessions.
`BatteryAppAttributionTest` covers the `sourceLabel` strings, the
`observedWindowMah` fallback chain, the proportional mAh scaling rules, the
apportion rounding-remainder distribution, the direct-to-observed scale,
the `sampleMah` linear-energy conversion, the per-app rate helpers and the
summary label paths. The full Direct-Debug unit-test suite (now 339
tests), build and lint passed locally. CI release workflow
[`35409811956`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35409811956)
passed and the signed tag is `v0.467`. Public update-repository commit
[`6d688ae`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/6d688ae)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`e5a6ced3d5e4173b1472508449042e03e85907482002978548bf6236e3126fbc` and
reports package `com.ampere.batterylab`, version code/name `467`/`0.467`.
Play internal-track publishing workflow
[`35410055087`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35410055087)
passed.

Last verified: 2026-09-19 (Europe/Berlin)

# Verification record

Version `0.466` adds three more regression suites so the data pipeline and
alarm triggers cannot silently drift. `BatteryExportRulesTest` covers the
legacy-telemetry validator (short rows, out-of-range levels, implausible
current, contradictory sign, malformed numbers) as well as the chronological
sort, the inclusive time window filter and the safe-by-default
`normalizeTelemetry` used by backup restore and migrations.
`BatteryChargeAlarmTest` pins the edge-triggered charge-target alarm
behaviour — including hysteresis, missing-baseline handling, "already
sent" suppression and invalid level rejection — so an off-by-one in the
upward-crossing test does not silence a legitimate target-hit notification.
`BatteryDischargeAlarmTest` locks down the threshold clamp range, the
edge-triggered low-battery alarm and the reset band after the threshold.
The full Direct-Debug unit-test suite (now 295 tests), build and lint
passed locally. CI release workflow
[`35408763547`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35408763547)
passed and the signed tag is `v0.466`. Public update-repository commit
[`caee6a5`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/caee6a5)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`b6d11c06eefe0cba8666164280a3c4544a2cd4f6bb6dcd075f3d74ef2c10b825` and
reports package `com.ampere.batterylab`, version code/name `466`/`0.466`.
Play internal-track publishing workflow
[`35409070005`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35409070005)
passed.

Last verified: 2026-09-19 (Europe/Berlin)

# Verification record

Version `0.465` adds regression coverage to four chart helper classes that
previously only lived inside `MainActivity` — `BatteryHistoryChartSelection`
hit-test boundaries, `BatteryCurrentChartWindow` date formatting and duration
rounding, `BatteryHistoryChartValues` empty-bucket fallbacks, and
`BatteryLevelChartSeries` daily grouping plus missing-day gap detection.
These helpers are the ones the chart rendering depends on, so locking their
behaviour in tests catches off-by-one and locale regressions early. The same
release also routes every status message in `UpdateChecker` through the
`Toasts` dedup helper so update-success and update-failure toasts no longer
stack when an in-app update flow is exercised in quick succession. The full
Direct-Debug unit-test suite (now 263 tests), build and lint passed locally.
CI release workflow
[`35384098397`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35384098397)
passed and the signed tag is `v0.465`. Public update-repository commit
[`e376c2d`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/e376c2d)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`400be4b5a1542e785226f819b52bd9efd4cc16831b250840f5705889330b188f` and
reports package `com.ampere.batterylab`, version code/name `465`/`0.465`.
Play internal-track publishing workflow
[`35384648388`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35384648388)
passed.

Last verified: 2026-09-19 (Europe/Berlin)

# Verification record

Version `0.464` addresses two issues spotted in a UI/UX scan of the
App-Verbrauch surface. The dashboard's per-row label and launcher icon were
resolved through `PackageManager.getApplicationLabel` and
`getApplicationIcon` once per row per `onDraw` frame — every invalidation
during a scroll or live update could fan out into hundreds of
ActivityManagerService round-trips. A new `AppLabelCache` memoises the
label and the icon per package in bounded LRU maps, and the row-drawing
helpers now call into it. In the same dialog pass, three `TextView`
close-style buttons (Settings "×", App-Verbrauch "‹", and App-Verbrauch
"SCHLIESSEN") had an `OnClickListener` but were not declared clickable,
so the dismissal never fired; the views are now explicitly
`setClickable(true)` and `setFocusable(true)`. New `AppLabelCacheTest`
cases guard the cache behaviour. The full Direct-Debug unit-test suite,
build and lint passed locally. CI release workflow
[`35381916030`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35381916030)
passed and the signed tag is `v0.464`. Public update-repository commit
[`7bde813`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/7bde813)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`cb16537ec6e106e88f09033e8c8e673054f168edad99f1b7ea7f9cdc062e71dc` and
reports package `com.ampere.batterylab`, version code/name `464`/`0.464`.
Play internal-track publishing workflow
[`35382641138`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35382641138)
passed.

Last verified: 2026-09-18 (Europe/Berlin)

# Verification record

Version `0.463` adds two UI/UX improvements spotted while reviewing the
status-surfacing code paths. Status toasts for backup, CSV export,
diagnostic export, research export, Akkustatus copied, "Live-Daten
aktualisiert" and the analytics/settings toggles are now routed through a
`Toasts` helper that coalesces identical translated strings inside a
two-second window, so rapid taps no longer stack overlapping toasts over
each other. The app-icon `ImageView` in the Entlade-Ansicht usage rows is
now explicitly marked decorative, so the screen reader does not double-name
each package next to its already-correct label. The changes are covered by
new `ToastsTest` cases plus the existing AppText coverage. The full
Direct-Debug unit-test suite, build and lint passed locally. CI release
workflow
[`35380300306`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35380300306)
passed and the signed tag is `v0.463`. Public update-repository commit
[`b095db6`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/b095db6)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`c6c3feb849084cd4254b1c1866d5f4e3e5f9215f386b82cb8d90f73696106495` and
reports package `com.ampere.batterylab`, version code/name `463`/`0.463`.
Play internal-track publishing workflow
[`35380890453`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35380890453)
passed.

Last verified: 2026-09-18 (Europe/Berlin)

# Verification record

Version `0.462` hardens the German history view against two bugs found in a
final-pass code scan. The chart's "EFC" and per-app "Rate" fallbacks used the
untranslated abbreviation `n/v` instead of the same `nicht verfügbar` phrase
already used everywhere else, and `selectedHistoryPeriod` returned `null` when
no usable telemetry rows existed, which the chart's `onDraw` consumed
unchecked. The chart now renders an empty summary instead of crashing, and both
labels spell the unavailability in full. `AppText.t` now translates both
phrases; `BatteryRulesTest` and `AppTextTest` guard the change. The focused
test, the full Direct-Debug unit-test suite, build and lint passed locally. CI
release workflow
[`35377461160`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35377461160)
passed and the signed tag is `v0.462`. Public update-repository commit
[`b24ff3a`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/b24ff3a)
publishes the artifacts. A fresh public APK download matches `latest.json` with
SHA-256
`7efc8f1d8487cf9fdbc99ce780d39185ca7a6f82ecb0962fb1694b3d2ce43963` and reports
package `com.ampere.batterylab`, version code/name `462`/`0.462`. Play
internal-track publishing workflow
[`35378904388`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35378904388)
passed.

Last verified: 2026-09-18 (Europe/Berlin)

# Verification record

Version `0.462` hardens the German history view against two bugs found in a
final-pass code scan. The chart's "EFC" and per-app "Rate" fallbacks used the
untranslated abbreviation `n/v` instead of the same `nicht verfügbar` phrase
already used everywhere else, and `selectedHistoryPeriod` returned `null` when
no usable telemetry rows existed, which the chart's `onDraw` consumed
unchecked. The chart now renders an empty summary instead of crashing, and both
labels spell the unavailability in full. `AppText.t` now translates both
phrases; `BatteryRulesTest` and `AppTextTest` guard the change. The focused
test, the full Direct-Debug unit-test suite, build and lint passed locally. CI
release workflow
[`35377461160`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35377461160)
passed and the signed tag is `v0.462`. Public update-repository commit
[`b24ff3a`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/b24ff3a)
publishes the artifacts. A fresh public APK download matches `latest.json` with
SHA-256
`7efc8f1d8487cf9fdbc99ce780d39185ca7a6f82ecb0962fb1694b3d2ce43963` and reports
package `com.ampere.batterylab`, version code/name `462`/`0.462`. Play
internal-track publishing workflow
[`35378904388`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35378904388)
passed.

Last verified: 2026-09-18 (Europe/Berlin)

# Verification record

Version `0.461` makes a full battery explicit in the widget. Android reports a
full battery as both `BATTERY_STATUS_FULL` and charging, so the widget now shows
`Voll geladen` and uses the neutral status color instead of the charging accent.
The rule is covered by `BatteryWidgetStatusTest`. The focused test, the full
Direct-Debug unit-test suite, build and lint passed locally. CI release workflow
[`35284381231`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35284381231)
passed and the signed tag is `v0.461`. Public update-repository commit
[`7e1d990`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/7e1d9904a10bf906088603e433ba99aa4e6a6b22)
publishes the artifacts. A fresh public APK download matches `latest.json` with
SHA-256
`6dc658f3706d8fc9bf59a5967097f332ee5c023eea82596ed69e434d722f64ac` and reports
package `com.ampere.batterylab`, version code/name `461`/`0.461`.

Last verified: 2026-09-17 (Europe/Berlin)

Version `0.460` improves the compact widget percentage readability. The value
column reserves more width, the percentage is larger, long status and telemetry
details now ellipsize instead of clipping, and the telemetry line is larger.
The change was verified in the API-36 emulator widget render and Direct-Debug
unit tests, build and lint passed locally. CI release workflow
[`35281986954`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35281986954)
passed and the signed tag is `v0.460`. Public update-repository commit
[`a4ad746`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/a4ad746caeda96d532470004b9ce5c6e4fe7fb25)
publishes the artifacts. A fresh public APK download matches `latest.json`
with SHA-256
`59da2c596d890113db85577495bbe5d5a9f42088e850e830ec0b5eecedbd7407` and reports
package `com.ampere.batterylab`, version code/name `460`/`0.460`.

Version `0.459` hardens English translation handling after a deeper language
scan. Complete translated phrases are now protected from later standalone-word
rules, preventing mixed-language and capitalization regressions in chart titles,
session labels, status messages and accessibility text. The full
`AppTextTest` suite, Direct-Debug lint and build passed locally. CI release
workflow
[`35277899496`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35277899496)
passed and the signed tag is `v0.459`. Public update-repository commit
[`1ae5efd`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/1ae5efd)
publishes the artifacts. The public APK matches `latest.json` with SHA-256
`846e6a184e11a7be6062d24743fef671e1350a99e747228eade6153344bdc7e7` and
reports package `com.ampere.batterylab`, version code/name `459`/`0.459`.
Play alpha publishing workflow
[`35278422792`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35278422792)
passed on retry.

Version `0.456` fixes additional mixed-language output found by a deeper scan,
including update errors, overlay text, charging/discharging status, missing
data, session summaries and settings labels. The scan also identified further
long-form privacy/accessibility strings that need a separate cleanup pass;
they are intentionally not treated as verified by this release. CI release
workflow
[`35274889696`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35274889696)
passed. Public update-repository commit
[`ee95ef1`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/ee95ef1)
publishes the artifacts. The public APK matches `latest.json` with SHA-256
`07b16943d771ca876e6f0cd387ddb0b67c5da01aacb623a0692e6292cb39c0ef` and
reports package `com.ampere.batterylab`, version code/name `456`/`0.456`.
Play alpha publishing workflow
[`35275562900`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35275562900)
passed.

Version `0.455` fixes the remaining mixed-language labels reported in the
English UI: battery temperature, current context, seven-day estimate,
recent sessions, more sessions in history and disconnected status. CI release
workflow
[`35273734562`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35273734562)
passed after correcting the translation-order regression. Public update-
repository commit
[`20ceb54`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/20ceb54)
publishes the artifacts. The public APK matches `latest.json` with SHA-256
`8b9f1579b8c75d90af137b50683f83d920e60ad0652517f04aa2ee7689934083` and
reports package `com.ampere.batterylab`, version code/name `455`/`0.455`.
Play alpha publishing workflow
[`35274370946`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35274370946)
passed.

Version `0.452` translates the live energy-flow heading (`LIVE ENERGY FLOW`)
and the excellent battery-health label (`Very good`) in English. Regression
tests cover both strings. CI release workflow
[`35272018692`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35272018692)
and Play alpha publishing workflow
[`35272858313`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35272858313)
passed. Public update-repository commit
[`a6d5f77`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/a6d5f77)
publishes the artifacts. The public APK matches `latest.json` with SHA-256
`e0998c1b828230f23cdd4911c490984dbc9734a0c259d35cd6c130f9a2af95fb` and
reports package `com.ampere.batterylab`, version code/name `452`/`0.452`.

Version `0.450` fixes the remaining mixed-language live usage status: the
English overview now shows “Usage is tracked live.” instead of “Usage wird live
erfasst.” A regression test covers the replacement-order case. The signed
`v0.450` release build passed CI; the initial asset attachment hit a GitHub
release-discovery race, was safely retried, and the final release contains APK,
AAB and checksums. Public update-repository commit
[`6cdbeb2`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/6cdbeb2)
publishes the artifacts. Fresh public APK download matches `latest.json` with
SHA-256 `1963ec1ae885f48c848cc0fbf047638eb60c69e5b7082bed6a78ad925e52180e`;
the public manifest reports package `com.ampere.batterylab`, version
code/name `450`/`0.450`. Play alpha publishing workflow
[`35270617319`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35270617319)
passed.

Version `0.449` fixes additional mixed-language English output found in the
charging, battery-health and history summaries, including “Measurement status”,
“used”, “battery efficiency” and duplicate punctuation. Direct and Play each
pass 220 unit tests; both debug lint tasks and release builds pass. The signed
`v0.449` tag is GitHub-verified, and release workflow
[`35268532155`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35268532155)
passed. Public update-repository commit
[`2b320f5`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/2b320f5)
publishes the artifacts. Fresh public downloads match `latest.json`: APK
SHA-256 `8c14292769bae88e220d0fbe79c39c8d7bc4eaf1ebf4596129027ccf742729f1`
and Play AAB SHA-256
`ac526c70fc31c0234514ab7b2a7566b61c2ae86bc47850ebaee0c916a2be9019`. The
public APK reports package `com.ampere.batterylab`, version code/name
`449`/`0.449`. Play alpha publishing workflow
[`35269160939`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35269160939)
passed.

Version `0.445` fixes mixed German/English battery-state phrases caused by
translation replacement order. “Akku ist voll”, “Akku wird geladen”, “Akku
entlädt sich” and “Akku fast leer” now render fully in English. The public
English APK was installed on the emulator and its live accessibility summary
was verified as “Battery is full”/English vocabulary with no mixed phrase.
Direct and Play each pass 220 unit tests and both debug lint tasks pass. The
signed `v0.445` tag is GitHub-verified, and release workflow
[`35266137145`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35266137145)
passed. Public update-repository commit
[`2eb2002`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/2eb2002)
publishes the artifacts. Fresh public APK download matches `latest.json`:
SHA-256
`dcefc12f608ab51c35d13173d58a1b3925933b1196e7c337cfb0c1370e93f834` (APK)
and `f9adf2bdab8be7e63fb9f3ce9e18d2b7b61d800d1e35cb7de27136f49f410063`
(AAB). The public manifest reports version code/name `445`/`0.445` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35266740057`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35266740057)
passed after a transient GitHub API 403 on the first attempt.

Version `0.444` fixes remaining German text in the English accessibility and
large-text summaries. Content descriptions now translate status, battery
values, tab names, active-tab text, the charge-target slider and navigation
controls consistently. Regression tests cover the generated English summary
phrases. Direct and Play each pass 220 unit tests and both debug lint tasks
pass. The signed `v0.444` tag is GitHub-verified, and release workflow
[`35264866576`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35264866576)
passed. Public update-repository commit
[`81ae0f4`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/81ae0f4)
publishes the artifacts. Fresh public APK download matches `latest.json`:
SHA-256
`b4f483eb057dbfac79b6da64bc74d644766c3674f0666c65faa1880b43125f7c` (APK)
and `235c089d1bc8e24781b9b982de0b090e02ea7ddc56c0dae69b6a3b1e66edaad1`
(AAB). The public manifest reports version code/name `444`/`0.444` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35265474428`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35265474428)
passed.

Version `0.443` completes English localization in secondary UI surfaces. The
update permission dialog, history bucket details and session details now use
the selected app language, including buttons and generated measurement text.
The English month-name replacement regression that turned “September” into
“Sepember” is fixed and covered by a regression test. Direct and Play each
pass 220 unit tests and both debug lint tasks pass. The signed `v0.443` tag is
GitHub-verified, and release workflow
[`35263245192`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35263245192)
passed. Public update-repository commit
[`9bf80e3`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/9bf80e3)
publishes the artifacts. Fresh public APK download matches `latest.json`:
SHA-256
`f8b357187c576730a12e2844bcbe2476602bfe2b222448752d009a0ebfec03ce` (APK)
and `2c14951ed690acd42a58c5b84c45a16f10bdc69ddd64f8b869bc8ca6a6b1c8da`
(AAB). The public manifest reports version code/name `443`/`0.443` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35263868211`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35263868211)
passed.

Version `0.441` fixes a remaining English-locale leak in live refresh
feedback. Both the normal live-refresh action and its accessibility action now
show “Live data refreshed.” instead of the German “Live-Daten aktualisiert.”;
the translation is covered by a regression test. Direct and Play each pass
220 unit tests and both debug lint tasks pass. The signed `v0.441` tag is
GitHub-verified, and release workflow
[`35261573877`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35261573877)
passed. Public update-repository commit
[`dbef7d8`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/dbef7d8)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`0f5dc33de1aee4647797eeb3504db918adb66bec96692c2a8a0c865a1f825668` (APK)
and `29c03cac38544d279625d4059daabd9970da4a31ba8f4318cad4a52846258ad0`
(AAB). The public manifest reports version code/name `441`/`0.441` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35262180594`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35262180594)
passed.

Version `0.440` improves discoverability of the app language switcher. The
Settings dialog now places “App language · English” or “App-Sprache · Deutsch”
as its first item, so the current language and the way to change it are
immediately visible. The emulator audit confirmed the German settings dialog
with the language entry first in
[`settings-top.png`](/tmp/ampere-language-first-item.png). Direct and Play
each pass 220 unit tests and both debug lint tasks pass. The signed `v0.440`
tag is GitHub-verified; the build completed all artifact steps, with the APK
attached directly after a transient release upload timeout. Public
update-repository commit
[`4d502eb`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/4d502eb)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`277b6854b05e36b39385f2a9d347d474b2cc9f23e328777eab19d83c82e06d6e` (APK)
and `96e343e3b590c39ab1eab9bf4caed3b08b0a3c6f9628b7c4d05ee4ef32df29d9`
(AAB). The public manifest reports version code/name `440`/`0.440` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35260928773`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35260928773)
passed.

Version `0.439` adds a working in-app language switcher under Settings → App
language. Users can switch between German and English without relying on the
Android system settings; the activity recreates with the selected locale on
Android 13+ and older supported versions. The emulator verified English →
German, including the full home screen and navigation labels. Direct and Play
each pass 220 unit tests and both debug lint tasks pass. The signed `v0.439`
tag is GitHub-verified, and release workflow
[`35257375721`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35257375721)
passed. Public update-repository commit
[`66bec98`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/66bec98)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`6cc72c80a3b5e70788ce40b77c08f1a9a316c80dacbee2d5011349e8856598dd` (APK)
and `806f935a887f51b7306b12fb214429870e60fe10f3b923c383a85b4032a58514`
(AAB). The public manifest reports version code/name `439`/`0.439` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35258101855`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35258101855)
passed.

Version `0.438` completes the English history chart labels. The monthly view
now translates the chart heading and calendar timeframe, including
“BATTERY BALANCE · 6 CALENDAR MONTHS”, instead of leaving German uppercase
labels in the English UI. The emulator audit confirmed the corrected History
view in [`history-fixed2.png`](/tmp/ampere-audit-ui-437-history-fixed2.png).
Direct and Play each pass 220 unit tests and both debug lint tasks pass. The
signed `v0.438` tag is GitHub-verified, and release workflow
[`35255766396`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35255766396)
passed. Public update-repository commit
[`926c532`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/926c532)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`1c160c843cb577889e2ba3e2370bd9718770dbad4a2992970142de9fea7cef76` (APK)
and `16411bc210bdd4d1a36af8b79dd95c951d77cdf07e10d68fbb7adb161446d1d4`
(AAB). The public manifest reports version code/name `438`/`0.438` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35256755624`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35256755624)
passed.

Version `0.437` fixes the last mixed-language entries found in the English
settings dialog: “Reset health baseline” and “Delete local data” now render
fully in English. The emulator audit confirmed the corrected bottom section in
[`04-settings-bottom-fixed.png`](/tmp/ampere-audit-ui-436/04-settings-bottom-fixed.png).
Direct and Play each pass 220 unit tests and both debug lint tasks pass. The
signed `v0.437` tag is GitHub-verified, and release workflow
[`35254458671`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35254458671)
passed. Public update-repository commit
[`85b5b6b`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/85b5b6b)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`441071b6b63c9134da1ed0241228a02f542f918ba53a0d19dc68af2d90411e00` (APK)
and `54288ddb1381c7b5e2a182fc323a446fd99fce9ffc5ac89476dae41302e273b5`
(AAB). The public manifest reports version code/name `437`/`0.437` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35255140931`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35255140931)
passed.

Version `0.436` localizes the remaining history accessibility path. Screen
readers now receive English dates, charged/used/wear labels and explicit empty
measurement explanations for chart buckets when the app is English. The
large-text navigation also exposes English action descriptions. Direct and
Play each pass 220 unit tests and both debug lint tasks pass. The signed
`v0.436` tag is GitHub-verified, and release workflow
[`35252954123`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35252954123)
passed after a transient release-asset retry. Public update-repository commit
[`18cc975`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/18cc975)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`18e8288c0066ae96d911718e2f337e672e6a4aeea7593ac7a6d330bdfaec3cb8` (APK)
and `4f0d2aa1aef4b8cdae5bb3fde1f34fe0d8401e389ee3397a2e4e2d2cb19b275c`
(AAB). The public manifest reports version code/name `436`/`0.436` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35254080957`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35254080957)
passed.

Version `0.435` extends the English UI to system-facing surfaces. The home
widget, Quick Settings tile, live overlay and charging screensaver now use the
active app locale for labels and decimal separators; the previous malformed
English compound for “Akkubetrieb” is covered by a regression test. Android
13+ can now expose Deutsch and English in the per-app language settings via
the locale configuration. The emulator audit confirmed the English home
screen in
[`02-home.png`](/tmp/ampere-audit-ui/02-home.png).
Direct and Play each pass 219 unit tests and both debug lint tasks pass. The
signed `v0.435` tag is GitHub-verified, and release workflow
[`35251758255`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35251758255)
passed. Public update-repository commit
[`00aeb67`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/00aeb67)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`5df7a9794f0efd303bc5b48955224a8596cc024564bc2d6fc4dbf64475f64093` (APK)
and `8f818fe386f4578b4e86002b468f4d2ee77a469097677b746fbe4ab64241d8a1`
(AAB). The public manifest reports version code/name `435`/`0.435` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35252450353`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35252450353)
passed.

Version `0.434` makes numeric and date formatting follow the active app
language. English history and status views now use English decimal/grouping
separators and date formatting, for example `0.54 EFC` and `1,000 mAh`
instead of German-formatted values. The emulator audit confirmed the English
History view in
[`04-history-locale.png`](/tmp/ampere-audit-next/04-history-locale.png).
Direct and Play each pass 218 unit tests and both debug lint tasks pass. The
signed `v0.434` tag is GitHub-verified, and release workflow
[`35249113008`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35249113008)
passed after a transient release-asset upload retry. Public update-repository
commit
[`7111657`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/7111657)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`83d9397f3e6277228accc45894622c363b6cf18ca8c5e5425a884f3c1ea28150` (APK)
and `6ca36da5710f122db742ac032b5d1a5c6e0c51e5ec5079bf681c2e63a970825b`
(AAB). The public manifest reports version code/name `434`/`0.434` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35250362909`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35250362909)
passed.

Version `0.433` localizes the remaining user-visible system surfaces for the
English locale: the live overlay and charge-target, high-temperature and low-
battery alarm notifications now translate their labels while preserving live
values such as CPU load, voltage and thresholds. Direct and Play each pass
218 unit tests and both debug lint tasks pass. The signed `v0.433` tag is
GitHub-verified, and release workflow
[`35248010726`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35248010726)
passed. Public update-repository commit
[`3c83954`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/3c83954)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`61bc31919868bd96aecc82d4622d3779b9bec4f99a805b367d0252a786177c2a` (APK)
and `91b51c80dcbcf1ee98108118f25c5afdc86f3e77706fd6dacc6bfefb19771ada`
(AAB). The public manifest reports version code/name `433`/`0.433` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35248654360`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35248654360)
passed after a transient public API 403 retry.

Version `0.432` clarifies empty discharge forecasts. When the device is
charging without a usable discharge history, each unavailable forecast now
shows the actionable hint “Start a discharge session” instead of the vague
source label “After unplugging”. The emulator audit confirmed the new state
on the Discharging page. Direct and Play each pass 218 unit tests and both
debug lint tasks pass. The signed `v0.432` tag is GitHub-verified, and release
workflow
[`35247092193`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35247092193)
passed. Public update-repository commit
[`412a7db`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/412a7db)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`f32fc7d00dca3d8a80e1d314d4e926cf85aabf00dacc8f9f0b886bf7cda254bb` (APK)
and `146eba2bd84a20a0688c7f25851640a493c7ca5aebf7f88026944e4cc39d1539`
(AAB). The public manifest reports version code/name `432`/`0.432` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35247668957`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35247668957)
passed.

Version `0.431` aligns the history summary cards with the visible chart
window. Daily, weekly and monthly views now aggregate the complete displayed
window, while the chart keeps its individual buckets; the range label names
that same window. The emulator audit confirmed the monthly view no longer
shows a single month's values under a six-month chart. Direct and Play each
pass 218 unit tests and both debug lint tasks pass. The signed `v0.431` tag is
GitHub-verified, and release workflow
[`35245871375`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35245871375)
passed. Public update-repository commit
[`43fabc0`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/43fabc0)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`1c672d3dc85a1a294f13b3e88eda986c5afdb44dba09531c7b2da5b2719d4ed0` (APK)
and `640559437875830211c3b798d841365e907c3ee3c64d9bd0cdb1fb07f8798dcc`
(AAB). The public manifest reports version code/name `431`/`0.431` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35246489865`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35246489865)
passed.

Version `0.430` completes the English UI pass for action feedback as well as
the main screens. Backup, export and measurement toasts, capacity dialogs,
copied status text and large-text actions now use the active English locale.
The translation regression suite also guards against broad word replacements
corrupting longer dialog text. Direct and Play each pass 218 unit tests and
both debug lint tasks pass. The signed `v0.430` tag is GitHub-verified, and
release workflow
[`35243922387`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35243922387)
passed. Public update-repository commit
[`f1e9260`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/f1e9260)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`cacb9c73f7ef7d9e611abec0901f9d51ae2a4f92c5b3254855a3da9f097974f6` (APK)
and `b62d30070ce4bd9a603c9d5d89ffbaacc060eb6d0bc0d88eb01cc1f59ce0875c`
(AAB). The public manifest reports version code/name `430`/`0.430` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35245005927`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35245005927)
passed.

Version `0.429` polishes the app-usage surface. System launcher packages now
use a human label (“Home screen”), the discharge summary uses “Battery level at
start” and “Current battery level”, and unavailable app rates are shown as
“Rate unavailable” instead of “Rate n/v”. The usage card's remaining mixed
German labels are now English as well, including “Screen”, “Usage” and
“estimated values”. Emulator screenshots confirmed the corrected usage card.
Direct and Play each pass 218 unit tests and both debug lint tasks pass. The
signed `v0.429` tag is GitHub-verified, and release workflow
[`35242434147`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35242434147)
passed. Public update-repository commit
[`ef5c3e8`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/ef5c3e8)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`39b2ea00e46d737eded4046e18825d269bd15bccbed33eea795462fd6af08f5d` (APK)
and `4832f52abc5ea4c7a83ed9015ef12f1badf23dfc0404648066259d2bd3bf7c4b`
(AAB). The public manifest reports version code/name `429`/`0.429` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35243246120`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35243246120)
passed.

Version `0.428` clarifies the history metric formerly shown as “Charged/Usage”
to “Charge/usage ratio”, so a value such as 712% is visibly identified as a
ratio rather than a second consumption amount. The history audit confirmed
that day, week and month cards and chart values change with the selected
period. Direct and Play each pass 218 unit tests and both debug lint tasks
pass. The signed `v0.428` tag is GitHub-verified, and release workflow
[`35241281976`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35241281976)
passed. Public update-repository commit
[`e70052b`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/e70052b)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`ae7da761b7f53e38c7fbbd5f747d0d58f705669ca5c12c70890db53cd24ee904` (APK)
and `a22cb2d0973fb8c71fcee5820ce2d4dcce6b588a308ee694179e93cd488e584d`
(AAB). The public manifest reports version code/name `428`/`0.428` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35241895260`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35241895260)
passed.

Version `0.427` completes the English health-page and navigation pass. The
health page now translates its learning/measurement copy, capacity labels,
system-cycle labels and the charging-progress connector. On narrow English
layouts, active navigation tabs use dynamic localized labels instead of
German text baked into bitmap artwork. Emulator screenshots confirmed
“Battery health”, “Battery” and “History” in the active navigation. Direct
and Play each pass 218 unit tests and both debug lint tasks pass. The signed
`v0.427` tag is GitHub-verified, and release workflow
[`35240216219`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35240216219)
passed. Public update-repository commit
[`af23e97`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/af23e97)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`5d791e33623f44c67de35ca20fd96e99f064b8a17cc81ef3742029831da3b239` (APK)
and `6ec5b3b0ad7ac2bc22f650e89edbda03cc32f0f87834015d4871eb8fc23b8b6c`
(AAB). The public manifest reports version code/name `427`/`0.427` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35240837034`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35240837034)
passed.

Version `0.426` completes another English UI/accessibility pass. The
dashboard now translates Screen time and keeps the history title as “Battery
level · 7 days”; the previous replacement-order bug produced “Batterystand ·
7 daye”. TalkBack labels for history periods, runtime forecasts, CSV export,
capacity actions and exact balance values are localized as well. The emulator
confirmed the corrected dashboard visually. Direct and Play each pass 218
unit tests and both debug lint tasks pass. The signed `v0.426` tag is
GitHub-verified, and release workflow
[`35239013511`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35239013511)
passed. Public update-repository commit
[`e878c1a`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/e878c1a)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`d161854ddc3d97b08b5b70009ddf4547c13e87aae7acbe3c11c24ea566b7ed9e` (APK)
and `41f0f41957928ca483962fd4083e02e84a68162418f12e25330b4e784ad59251`
(AAB). The public manifest reports version code/name `426`/`0.426` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35239641979`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35239641979)
passed.

Version `0.425` clarifies the discharging page when the device is charging:
the page now distinguishes the last discharge summary from a ready-to-start
state instead of presenting a misleading daily-pattern title. App-usage
details also use the complete English terminology, including telemetry and
foreground-time estimates, unavailable-state text and dialog actions. The
emulator confirmed the new “Last discharge” state visually; direct and Play
each pass 216 unit tests and both debug lint tasks pass. The signed `v0.425`
tag is GitHub-verified, and release workflow
[`35237521603`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35237521603)
passed. Public update-repository commit
[`e97e971`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/e97e971)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`001d7bb0b25113ae9a3f3f0792d4e160cc720827b95810f5f05f66bec795440b` (APK)
and `a7798e937c09b646a19df2a126f538eb1fde03a5d6ba4e8483a0e866ec214231`
(AAB). The public manifest reports version code/name `425`/`0.425` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35238099134`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35238099134)
passed.

Version `0.424` completes the English history-chart polish and fixes a
layout overlap in the history page. Month and weekday labels, chart legends
and values now remain English; the Recent Sessions section starts below the
Analysis card instead of drawing over it. Emulator screenshots confirmed the
monthly chart and the separated sections. Direct and Play each pass 216 unit
tests and both debug lint tasks pass. The signed `v0.424` tag is
GitHub-verified, and release workflow
[`35236306297`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35236306297)
passed. Public update-repository commit
[`642932f`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/642932f)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`5dc8141145c8aeb7e25c7d172f1ac5eca05e5c9c385952dcae0f26aa23a9ca47` (APK)
and `77657349a618453c1daad5552d58958c449629466b983b13f20ab6557a1a33b7`
(AAB). The public manifest reports version code/name `424`/`0.424` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35236865020`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35236865020)
passed.

Version `0.423` improves the start page with state-aware messaging (for
example, a full battery now says “Battery is full” instead of claiming that
everything is simply running smoothly). The translation layer now respects an
app-specific English locale even when the device-wide locale is different.
The emulator confirmed the English full-battery state visually; direct and
Play each pass 216 unit tests and both debug lint tasks pass. The signed
`v0.423` tag is GitHub-verified, and release workflow
[`35234819622`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35234819622)
passed. Public update-repository commit
[`6c793a9`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/6c793a9)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`66620abfde1704ff204a7f61dd805cd7eb4585c17e351cbcbda4ddb47c933a7d` (APK)
and `87acb0c3ede9179ee3fa22b38ffbcce45b2150926c80ee92d8422e3a2538aea5`
(AAB). The public manifest reports version code/name `423`/`0.423` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35235474788`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35235474788)
passed.

Version `0.422` completes the English system-surface pass: live-overlay and
update notification channels, Quick Settings text, update dialogs and
background-monitoring status are localized consistently with the device
language. The emulator showed the English navigation labels; direct and Play
each pass 216 unit tests and both debug lint tasks pass. The signed `v0.422`
tag is GitHub-verified, and release workflow
[`35233400138`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35233400138)
passed. Public update-repository commit
[`8456967`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/8456967)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`543451bee3b38f7408c06b533dd8dbeff6005fc5816b2d7b89999f900522b16c` (APK)
and `ddb94056809289c9082dfcb4cbef00b2f4e60d5ce3a90e9b8c14e0462dac73bc`
(AAB). The public manifest reports version code/name `422`/`0.422` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35234143660`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35234143660)
passed.

Version `0.421` completes the English live-status pass: foreground-service
notification details, technical labels and notification-channel names now
follow the device language, and the ongoing notification explicitly states
that it is updated every second. Local emulator inspection confirmed the
service path; direct and Play each pass 216 unit tests and both debug lint
tasks pass. The signed `v0.421` tag is GitHub-verified, and release workflow
[`35231949388`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35231949388)
passed. Public update-repository commit
[`bc2b38a`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/bc2b38a)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`de3ed6302d68f253fee6c8cbd2b208726b95e3c3bb716c900f11af7b498edeec` (APK)
and `eee79c933dfc67e4d52ad0abd6482dc55c304dc7311733d44facb6eb29ed92ce`
(AAB). The public manifest reports version code/name `421`/`0.421` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35232625519`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35232625519)
passed.

Version `0.419` completes the English settings-language pass for privacy,
usage analytics, backup/restore, local-data deletion and health-baseline
dialogs. The emulator confirmed the long privacy dialog is readable and its
actions are English. Direct and Play each pass 216 unit tests; both debug
lint tasks pass. The signed `v0.419` tag is GitHub-verified, and release
workflow
[`35230732777`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35230732777)
passed. Public update-repository commit
[`cf1ff5c`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/cf1ff5c)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`a710556e4d75239231c751d8992513dff2b13ac1d2b671be6657990932ef2cd4` (APK)
and `d85bf39515acf90caa92e4e15ee78cf85385ff63e735dbbc40bcb1e12aa97c83`
(AAB). The public manifest reports version code/name `419`/`0.419` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35231413467`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35231413467)
passed.

Version `0.418` improves the English onboarding and permission feedback and
removes mixed-language history-card labels caused by replacement ordering.
Current emulator screenshots confirm the English settings, permission review
and history surfaces. Direct and Play each pass 216 unit tests; both debug
lint tasks pass. The signed `v0.418` tag is GitHub-verified, and release
workflow
[`35229668895`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35229668895)
passed. Public update-repository commit
[`89d548e`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/89d548e)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`7567e62b9afe01f7b39bf11e87a469bf721bf779415e578fdc1080171edc104c` (APK)
and `5b0a7b96b19cf885208c439db3085a5af4acd6be0dce8a4ded95b330b6272973`
(AAB). The public manifest reports version code/name `418`/`0.418` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35230274261`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35230274261)
passed.

Version `0.417` completes the English UI pass through settings, permission
review and background-monitoring status. The emulator confirmed the settings
dialog in English, including dynamic temperature, low-battery and sampling
labels. Direct and Play each pass 216 unit tests; both debug lint tasks pass.
The signed `v0.417` tag is GitHub-verified, and release workflow
[`35227932669`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35227932669)
passed. Public update-repository commit
[`5aa3b77`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/5aa3b77)
publishes the artifacts. Fresh public APK and AAB downloads match
`latest.json`: SHA-256
`ede98fb37e59dd614162036b9cd26207c85454d7e03d778cd4e0962a7bfbfa1b` (APK)
and `37692cc3a3f989fe6dd54cdc290d68b6b97946b56a16726c63ff9c0550b2f64e`
(AAB). The public manifest reports version code/name `417`/`0.417` and the
package remains `com.ampere.batterylab`. Play alpha publishing workflow
[`35228651750`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35228651750)
passed.

Version `0.416` continues the English UI pass through charging, health and
technical dashboard cards, including charge companion status, charge target
progress, alert/overlay descriptions, sensor headings and capacity guidance.
Direct and Play each pass 216 unit tests; both debug lint tasks pass. The
signed `v0.416` tag is GitHub-verified, and release workflow
[`35226542919`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35226542919)
passed. Update-repository commit
[`b64c31d`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/b64c31d)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`62e2a57600f07544250ebe49794af1e8cbb77032a06c7e0fee5ee80700aee091` (APK) and
`249a5f182bcf2f6a8791af3ed930836ec15f48b0ff4126b656700d7588448719` (AAB).
The APK is `com.ampere.batterylab`, version code/name `416`/`0.416`. Play alpha
publishing workflow
[`35227046071`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35227046071)
passed.

Version `0.415` continues the English UI pass across the discharge and history
pages, including forecast modes, session labels, battery-pattern cards, app
usage actions, chart headings and accessibility control labels. Direct and
Play each pass 216 unit tests; both debug lint tasks pass. The signed `v0.415`
tag is GitHub-verified, and release workflow
[`35225486644`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35225486644)
passed. Update-repository commit
[`971a72c`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/971a72c)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`8a7f08b54bf134db7b00dcde782c79bf97b71038bb2c00fac62e221981684586` (APK) and
`8c823946382bedd9c8e7fcec2b35d5d13a5a700f233a65e90122e70f0e923d55` (AAB).
The APK is `com.ampere.batterylab`, version code/name `415`/`0.415`. Play alpha
publishing workflow
[`35226103275`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35226103275)
passed.

Version `0.414` fixes the remaining English empty-state and status labels
found during emulator review, including the battery-level heading, charging
state, health card, measurement action, capacity labels and the dashboard
headline. The emulator confirmed the English start screen without German
residual labels. Direct and Play each pass 216 unit tests; both debug lint
tasks pass. The signed `v0.414` tag is GitHub-verified, and release workflow
[`35224283221`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35224283221)
passed. Update-repository commit
[`8945124`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/8945124)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`1db0a788dc64edb6fb6cc56ce11fb848f8d1454c5deabb36e4ce49d1756fa5f5` (APK) and
`12d89d3146e0b0925953a76b46bc464002faca37e18d727fc017210850175e01` (AAB).
The APK is `com.ampere.batterylab`, version code/name `414`/`0.414`. Play alpha
publishing workflow
[`35224895996`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35224895996)
passed.

Version `0.413` adds a complete English UI layer selected from the device
language. The custom dashboard, large-text mode, app-usage view, widgets, live
monitoring notification and overlay notification translate their visible
battery vocabulary while preserving measurements and app names. Direct and
Play each pass 216 unit tests; both debug lint tasks pass. The signed `v0.413`
tag is GitHub-verified, and release workflow
[`35223121207`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35223121207)
passed. Update-repository commit
[`6d14c45`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/6d14c45)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`8daeb7e9c3eb19f8b4b82885b9c1fc7b7b31c96fec4b296223e81632dd8e8053` (APK) and
`fab3df6094662209823d98dbd6e7be78d6c45d29c9a04ab7d79a2092fee1872f` (AAB).
The APK is `com.ampere.batterylab`, version code/name `413`/`0.413`. Play alpha
publishing workflow
[`35223671829`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35223671829)
passed.

Version `0.412` adds the real launcher icon beside each app in the “Akkuverbrauch
deiner Apps” summary. This improves visual scanning while keeping estimated
values explicitly marked with `~`. Direct and Play each pass 216 unit tests;
both debug lint tasks pass. The signed `v0.412` tag is GitHub-verified, and
release workflow
[`35221220036`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35221220036)
passed. Update-repository commit
[`85082ad`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/85082ad)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`1091b4015e8dde16084062c5f372ebf7fb00bcbba64de811657c099ccb473c9f` (APK) and
`d005deb50e91feab448ea78819533794fa12dcd8a52d32070d7071a6786df18c` (AAB).
The APK is `com.ampere.batterylab`, version code/name `412`/`0.412`. Play alpha
publishing workflow
[`35221808763`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35221808763)
passed.

Version `0.411` fixes the focused history chart axis: short measurement
windows now show clock times instead of repeating the same weekday at every
tick. The emulator confirmed the visible `12:43 · 13:09 · 13:36 · 14:03`
labels. Direct and Play each pass 215 unit tests; both debug lint tasks pass.
The signed `v0.411` tag is GitHub-verified, and release workflow
[`35220093642`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35220093642)
passed. Update-repository commit
[`7f23454`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/7f23454)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`8224102e22f12603b7a0ae15e587a5c40acc54bf7e97b6b8f57078ed47dc00c6` (APK) and
`dc0d3488b4846eb22572846f0659301261c3d61193b58036cafa0ce50cad1cbf` (AAB).
The APK is `com.ampere.batterylab`, version code/name `411`/`0.411`. Play alpha
publishing workflow
[`35220566780`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35220566780)
passed.

Version `0.410` keeps long duration values in the dashboard metric cards fully
visible with a dedicated readable size, including the “Dein Tagesrhythmus”
area; the diagnostic renderer no longer adds a truncation marker. Direct and
Play each pass 214 unit tests; both debug lint tasks pass. The signed `v0.410`
tag is GitHub-verified, and release workflow
[`35218917665`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35218917665)
passed. Update-repository commit
[`d43624d`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/d43624d)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`caa9617322f4b0866c98f9e72d5ff9761c908eeacb21a40e194e7c5dcb99eca6` (APK) and
`f90c916c95ee0bb90b0075134fa1da49cfbc3e21c6998e33f04c3f048958d7bc` (AAB).
The APK is `com.ampere.batterylab`, version code/name `410`/`0.410`. Play alpha
publishing workflow
[`35219517255`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35219517255)
passed.

Version `0.409` adds a visible freshness label to the live energy-flow card,
so the current sensor reading is explicitly marked as recent or aged. Direct
and Play each pass 213 unit tests; both debug lint tasks pass. The emulator
confirmed the visible `Messung gerade eben` state in the live card. The signed
`v0.409` tag is GitHub-verified, and release workflow
[`35217736616`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35217736616)
passed. Update-repository commit
[`710264a`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/710264a)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`164dff6c3a77b1509cfbfc961319e5ef1982eee3f05cedd907406fb3e5ce7c0f` (APK) and
`6af290d2c7a0f71f689e23fa0164ff0c29162bd742a0ec0c5d359013aba8dca7` (AAB).
The APK is `com.ampere.batterylab`, version code/name `409`/`0.409`. Play alpha
publishing workflow
[`35218283962`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35218283962)
passed.

Version `0.408` wraps long app names and usage details over two lines instead of
ellipsizing them. Direct and Play each pass 211 unit tests; both debug lint
tasks pass. The signed `v0.408` tag is GitHub-verified, and release workflow
[`35216844013`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35216844013)
passed. Update-repository commits
[`58b8bd2`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/58b8bd2)
and
[`6509ef4`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/6509ef4)
publish the manifest and artifacts. Fresh public APK and AAB downloads match
manifest SHA-256 values
`898b0b807343d0d6c04bb1c894a5bfccc891520db8da4a5eed729e26669a6c0e` (APK) and
`1723ae7a56a0726f8c85d52ef458a687cbeda11858eb23b7a0dc7c34eda94156` (AAB).
The APK is `com.ampere.batterylab`, version code/name `408`/`0.408`. Play alpha
publishing workflow
[`35217315729`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35217315729)
passed.

Version `0.407` makes the values below the history bars larger, explains that
bar height is scaled per metric, and hints that the large-text navigation tabs
can scroll horizontally. Its emulator audit at 320×640 verified the dashboard,
monthly-history selection and value chart, as well as the native large-text
reflow at 150% font scale. Direct and Play each pass 210 unit tests; both debug
lint tasks pass. The signed `v0.407` tag is GitHub-verified, and release
workflow
[`35215719696`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35215719696)
passed. Update-repository commit
[`ee46a11`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/ee46a11)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`9a7cb2757ac88b46eb03a815f5ecb674580280f25cec86c14541edcd31bd8568` (APK) and
`a5a691f9a546e384a1f72ef2661d0e2668d91d21e6cb29a72b620b351415f486` (AAB).
The APK is `com.ampere.batterylab`, version code/name `407`/`0.407`. Play alpha
publishing workflow
[`35216260547`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35216260547)
passed.

Version `0.406` removes text ellipsizing from the shared canvas labels and
system battery widgets: long values are fitted to their lane instead of being
replaced by three dots. Direct and Play each pass 209 unit tests; both debug
lint tasks pass. The signed `v0.406` tag is GitHub-verified, and release
workflow
[`35214187427`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35214187427)
passed. Update-repository commit
[`09816f0`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/09816f0)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`d675c2a2b676323148ee27f3b130b63adb2ff730ec28ff29045bd49cf0f447d9` (APK) and
`7316c2b23abaa42f83d365d8494e4f38d85f1c28486944a32c61d1f3f11a0fc8` (AAB).
The APK is `com.ampere.batterylab`, version code/name `406`/`0.406`. Play alpha
publishing workflow
[`35214750606`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35214750606)
passed.

Version `0.405` labels the discharge-current chart with the exact start and end
time, elapsed duration and number of plotted local samples (including a date
when samples cross midnight, and an explicit momentary-sample label). Direct
and Play each pass 209 unit tests; both debug lint tasks pass. Signed release,
public APK/manifest and Play alpha publication are verified. The signed `v0.405`
tag is GitHub-verified, and release workflow
[`35213018511`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35213018511)
passed. Update-repository commit
[`55f3200`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/55f3200)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values
`7bea6fd37da65ca4448620a48e87b71d2a3c4d1ca1005a0d3d200e870f9b1a02` (APK) and
`2a850ddb9bcffcd420418899f5848686b4328db4a0ca959b03a4f92981c1871f` (AAB).
The APK is `com.ampere.batterylab`, version code/name `405`/`0.405`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35213529756`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35213529756)
passed.

Version `0.404` redesigns the discharge-page app-use card as an inline
"Akkuverbrauch deiner Apps" preview with the top three apps ranked by estimated
consumption, comparable bars, foreground time and the available mAh/h rate.
The optional usage-access explanation is clearer, and the entire card still
opens period-filtered details. The full app list clarifies that per-app values
are estimates, not exact Android measurements. Direct and Play each pass 208
unit tests; both debug lint tasks and the direct debug build pass. The 320×640
API 36 emulator confirmed the compact card layout and optional-access state;
it had no real app-usage history for a populated top-app preview. The signed
`v0.404` tag is GitHub-verified, and release workflow
[`35212228880`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35212228880)
passed. Update-repository commit
[`2317b5b`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/2317b5b)
publishes the artifacts. Fresh public downloads match manifest SHA-256 values
`2e3896ea994df746e4925412d9a3b1958c6e5e137451d7cb264dc95d7a101135` (APK) and
`5bec4bcce68a636be92e7cc4f1ffc0e7fdfc1b76b26c4a3a26e06aebc663f9a4` (AAB).
The APK is `com.ampere.batterylab`, version code/name `404`/`0.404`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35212722531`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35212722531)
passed.

Version `0.403` aligns the dashboard's 7-day/30-day button hitboxes with their
drawn positions and fixes the chart's vertical hitbox origin in compact layouts.
Touch, TalkBack and selected-range feedback now use the same bounds. Direct and
Play each pass 208 unit tests; both debug lint tasks pass. The signed `v0.403`
tag is GitHub-verified, and release workflow
[`35210773409`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35210773409)
passed. Update-repository commit
[`ef397da`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/ef397da)
publishes the artifacts. Fresh public downloads match manifest SHA-256 values
`80064c4da41d94bd186c21d732762200cf61b7e3b08a6ec15611ee893377e59e` (APK) and
`321ab8e24adb65ddb7a7cd535114c8b6e1d4e626c967f78f3f18922395d314f5` (AAB).
The APK is `com.ampere.batterylab`, version code/name `403`/`0.403`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35211181290`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35211181290)
passed.

Version `0.402` fixes the home dashboard's screen-time tile: short durations
use compact hour/minute labels (for example, `16h 22m`) so the full value is
visible instead of ending in an ellipsis. Direct and Play each pass 207 unit
tests; both debug lint tasks pass. The signed `v0.402` tag is GitHub-verified,
and release workflow
[`35210016995`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35210016995)
passed. Update-repository commit
[`a38ea27`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/a38ea27)
publishes the artifacts. Fresh public downloads match manifest SHA-256 values
`5c116f1bd22163fdcbaeecde6335b75d6aa00efde8a259fd022b190e29677969` (APK) and
`40e6f0474578bebc8db769fa548dcb1bdc92b713dc50789348915572448dee91` (AAB).
The APK is `com.ampere.batterylab`, version code/name `402`/`0.402`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35210361815`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35210361815)
passed.

Version `0.401` completes the two-step first-run guide by requesting Android's
notification permission only after the user chooses “Loslegen”. App-usage and
overlay access remain optional and in-context. Opening the checklist starts
the reminder cooldown, preventing an immediate duplicate after denial; a
previously granted optional access is now treated as tracked even if it was
enabled outside Ampere, so its later revocation is detected. Direct and Play
each pass 206 unit tests; both lint tasks and both debug APK builds pass. A
local Android emulator was unavailable for visual verification. GitHub verifies
the signed `v0.401` tag, and release workflow
[`35208960083`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35208960083)
passed. Update-repository commit
[`93ef3bb`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/93ef3bb)
publishes the artifacts. Fresh public downloads match manifest SHA-256 values
`616c22dfbb18b55a9f8988222e24fc2488ac0cd29f9d5bea655d11fc6957418d` (APK) and
`a183beca374fa3fbaa63d163e5a45c275b082c4ca912fcc86a56cf2ea3f24fcc` (AAB).
The APK is `com.ampere.batterylab`, version code/name `401`/`0.401`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35209495681`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35209495681)
passed.

Version `0.400` prints the exact charge, consumption, wear and charge-ratio
values below each daily, weekly and monthly chart group, aligned by period and
colored in the same order as the legend. Missing periods and unavailable wear
or ratio data remain `—`; measured zeroes are shown as `0`. The chart card was
expanded to contain the added value rows. Direct and Play each pass 205 unit
tests; both lint tasks and both debug APK builds pass. A local Android emulator
was unavailable for a visual capture. The signed `v0.400` tag is GitHub-verified,
and release workflow
[`35207061846`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35207061846)
passed. Update-repository commit
[`17be612`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/17be612)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values `084fdbaed5682e6ca08bb7a7468faffea1a77ee31d149e8faeef745290417a0e`
and `81be18e97d0ca8935ab7b2ac2a7f1715d830c11410d4f6574f20e7ebb540da22`.
The APK is package `com.ampere.batterylab`, version code/name `400`/`0.400`,
signed by the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35207907704`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35207907704)
passed after switching bundle retrieval to the GitHub Contents API to avoid
stale CDN responses.

Version `0.399` moves the permission-status checklist from the eleventh settings
position to the second, immediately after Notifications. In a fresh API-36
emulator audit at 320 × 640 dp, the old row was below the first visible menu
page; after the change it is visible at both normal and 130% system font scale.
Live taps opened the permission checklist and, separately, still routed the
following "Ladeziel & Ladealarm" item to the Charge page after menu indices
shifted. At 130% font scale, permission states, optionality, explanatory copy,
and the Finish button remained visible and readable. Direct and Play each pass
203 unit tests; both lint tasks and debug APK builds pass. The signed tag
`v0.399` is GitHub-verified. Release workflow
[`35166613585`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35166613585)
passed; update-repository commit
[`60da845`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/60da845)
publishes the artifacts. Fresh public APK and AAB downloads match manifest
SHA-256 values `450ea62d8d7214776966700e45d4e08f05b3cb231d832ff844848ae360b20afe`
and `e1c73be9056b296ab6013c71a123eca5f6cbcd26b0a8204ded3bc1f8fd31c004`.
The APK is package `com.ampere.batterylab`, version code/name `399`/`0.399`,
signed by the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35206188016`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35206188016)
passed. The workflow's Play Action configuration now uses the upstream-supported
`tracks` input instead of deprecated `track`; credential, bundle and manifest
validation passed in a non-uploading check
[`35206375898`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35206375898).

Version `0.398` fixes three narrow-screen usability defects found in the live
API-36 emulator audit: Canvas navigation targets were 40 dp high (and under
48 dp wide at 240 dp), the page title clipped at 240 dp, and the health/discharge
illustration could overlap its primary copy. Targets now expose at least 48 ×
48 dp at 240, 250, 275, 276 and 320 dp, and live taps at 240 dp reached both
Health and History. The title remains whole at 240 dp; the decorative figure
recedes below 320 dp; charging forecasts now say "Nach dem Abstecken" when no
discharge data exists yet. Direct and Play each pass 202 unit tests; both lint
tasks and debug APK builds pass. The signed tag `v0.398` is GitHub-verified.
Release workflow
[`35165504218`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35165504218)
passed, and the public artifacts in update-repository commit
[`21abd3e`](https://github.com/Apfelkringel/ampere-battery-lab-updates/commit/21abd3e)
were downloaded again and matched the manifest hashes: APK
`df00fd3bd8a8633ebf0b973c297de2eadf40ea4eff4431343b762d31ebf71bed`, Play AAB
`0b0e6d16309b10845303e4cb3b48ed711e396e8657fbe86e47ae52d1b61889bf`. The APK
package is `com.ampere.batterylab`, version code/name `398`/`0.398`, signed by
the expected certificate SHA-256
`301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`. Play
alpha publishing workflow
[`35165941677`](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/35165941677)
also passed.

Version `0.397` switches to a native, scalable, reflowable dashboard when the
system font scale is at least 1.25; the normal-size illustrated Canvas UI is
unchanged. On a 320×640 API-36 emulator at 200% font scale, the title and
summary wrapped, the navigation remained horizontally scrollable, and daily
and weekly history actions were visible and tappable; the remaining history
actions are reachable by vertical scrolling. Changing the font scale back to
100% recreated the activity and restored the selected History tab. Direct and
Play each pass 199 unit tests (0 failures); both lint tasks and debug APK
builds pass. Release workflow `35163373951` succeeded, and GitHub verified the
SSH-signed `v0.397` tag (`94a1075`). The public update repository was updated
in commit `725fa2d`; APK and AAB SHA-256 values are
`bf4781f66880fbaeba8efcfd0cbe180171df600c7a3b16841edfa818dda950a0` and
`4ef329638305d20a10b282b92b11a00cf3deb1ae109ee8f99cd53e171b4d1eff`.
Live downloads match `latest.json`. The APK reports package
`com.ampere.batterylab`, version `0.397` (versionCode 397), and the expected
release certificate SHA-256 `301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`.
Google Play upload workflow `35163813206` completed on the alpha testing track.

Version `0.396` exposes every daily, weekly, and monthly balance-chart bucket
as its own focusable, clickable accessibility item. Each item announces its
period and actual charge, consumption, wear, and ratio values; periods without
measurements explicitly say that readings are missing rather than zero. On a
320-dp API-36 emulator, the accessibility hierarchy exposed all six monthly
buckets, including the measured September value and five missing months. The
same interaction geometry is used for touch exploration and chart taps. The
portrait-only manifest and UX documentation are also protected by a regression
test. Direct and Play each pass 195 unit tests; both lint tasks and both debug
APK builds pass locally. Release workflow `35161104787` succeeded, and GitHub
verifies the signed `v0.396` tag. The public update repository was updated in
commit `9f1768b`; APK and AAB SHA-256 values are
`51f01c7fd57a2a8df7ae642c956f12dae7c049bb326c8fff1c63c003458f950f` and
`e73e75def9944a9c15d91add50ddcd55506936a1caad29445a0e77f4c3a9cef1`.
Live downloads match `latest.json`. The APK reports package
`com.ampere.batterylab`, version `0.396` (versionCode 396); the signed release
workflow verified the expected signing certificate. Google Play upload
workflow `35161551564` completed on the alpha testing track.

Version `0.395` makes the Akku-Bilanz's normalized bars interpretable at a glance:
the legend gives each series its actual maximum and unit, with charge and
consumption scaled independently. Missing readings remain `—`, and tapping a
bar still opens its exact values. On a 320-dp API-36 emulator, the daily and
monthly charts were visually checked and the monthly period showed six calendar
months with the correct per-series scale. Direct and Play each pass 191 unit
tests; both lint tasks and both debug APK builds pass locally. Release workflow
`35159724591` succeeded and GitHub verifies the SSH-signed `v0.395` tag. The
public update repository was updated in commit `0def856`; the APK and AAB
SHA-256 values are `c3d6e33975db1e6da4a0eeb7b2f407133742e4214bb80404c737aa1e752f9e32`
and `941244e3034c1065a3b9a7f761249cc3b0685d2b06964228cb492f0ce5451cab`.
Live public downloads match `latest.json`. The APK reports package
`com.ampere.batterylab`, version `0.395` (versionCode 395), and the expected
signing certificate. Google Play upload workflow `35160194493` completed on
the alpha testing track. Play Console currently shows release `0.395` as under
review; `0.383` remains available to testers until Google completes review.

Version `0.394` fixes the permission checklist: its three actionable access
rows now appear together with a concise explanation, show the current Android
grant state, and remain first in the scrollable dialog so they are reachable
at large font sizes. On a 320-dp API-36 emulator, the list was checked at the
default and 200% font scales; all three rows appeared in the accessibility
hierarchy, opening app-usage settings worked, and returning after granting
access updated its status to `Aktiv`. Direct and Play each pass 189 unit tests;
both lint tasks and debug APK builds pass locally. Signed release workflow
`35156891195` succeeded and GitHub verified signed tag `v0.394`. Internal
Google Play draft upload `35157453608` succeeded. Public APK/AAB SHA-256 values
are `9b80338807ed524bb0c5175150850549646295e007e5c963655fcbbf39b8a967` and
`b6f536e037be74b88e1ac840c4206fb1f1dc6c561de858a9a1f9dfac2fa8f32e`;
downloaded artifacts match the public manifest. The APK reports package
`com.ampere.batterylab`, version `0.394` (versionCode 394), and the expected
signing certificate.

Version `0.393` lays out discharge-runtime forecasts as three larger, readable
rows on narrow screens and exposes each mode as its own non-interactive
accessibility node. When an estimate is unavailable, its screen-reader entry
also states the next step (such as completing a discharge or gathering more
data). On a 320-dp API-36 emulator, the updated card was visually checked and
TalkBack's accessibility hierarchy exposed each forecast and hint separately.
Direct and Play each pass 188 unit tests; both lint tasks and both debug APK
builds pass locally. Signed release workflow `35154827377` succeeded and
GitHub verified signed tag `v0.393`. Internal Google Play draft upload
`35155364137` succeeded. Public APK/AAB SHA-256 values are
`56b312b5b7c0487e8049547ea9f64560e6e0130b55f3da16d7befe22af08e98d` and
`10cc406f903f30a5ffc08db96a3b5da0b7179da2b2580751d34a3b01b7a130ed`;
downloaded artifacts match the public manifest. The APK reports package
`com.ampere.batterylab`, version `0.393` (versionCode 393), and the expected
signing certificate.

Version `0.392` makes unavailable battery-runtime forecasts actionable without
inventing estimates: the screen-on/off modes say when an initial discharge or
five minutes of suitable measurements are needed, and normal-use mode asks for
more data. The display-only hint logic is regression-tested. Direct and Play
each pass 186 unit tests; both lint tasks and both debug APK builds pass. The
signed release workflow `35152704303` succeeded; GitHub verifies signed tag
`v0.392`. Internal Google Play draft upload `35153289445` succeeded. Public
APK/AAB SHA-256 values are
`f464ed9210831736c327af9b6e575a1abffaf0ab53dbd549d5cfcc3cf1643ef4` and
`7a31803e382f4f08eb8959fa08bc887bceed38b54d9467b7e4ac42e2256558f6`;
downloaded artifacts match the public manifest. APK package/version are
`com.ampere.batterylab`, `0.392` (versionCode 392), and the expected signing
certificate.

Version `0.391` replaces the blocking first-start analytics prompt with a
two-step app tour, preserves optional permissions as optional, and limits
missing-access reminders to once per 30 days (detected revocations remain
immediate). History bars now open exact bucket values and disclose when
measurement intervals are missing rather than reporting a misleading zero.
On a 320-dp API-36 emulator, both intro steps, overview and tappable history
details were visually checked. Direct and Play each pass 185 unit tests; both
lint tasks and both debug APK builds pass. Signed release workflow
`35151240912` succeeded; GitHub verifies signed tag `v0.391`. Internal Google
Play draft upload `35151847811` succeeded. The public APK/AAB SHA-256 values are
`ac94803f5b684664c1371bb9bee6d8cbcf62a7b6ea0cb7f95d13736763e9c8ed` and
`964dc4dc51d310b11e8eb49e1159e3053051b2ea50e4e4a47b0769e6e64e27eb`;
downloaded public artifacts match the manifest. The APK reports package
`com.ampere.batterylab`, version `0.391` (versionCode 391), and the expected
signing certificate.

Version `0.390` introduces a first-open guide explaining the five main app
areas and what each access enables. The permission checklist distinguishes
notification permission (including app/channel notification switches) from
optional usage access and overlay access, links each item to the relevant
Android screen, and checks actual system state on every resume. Missing access
is reminded weekly at most; a detected revocation is surfaced immediately.
Optional-access reminders begin only after the user opens that feature or its
permission entry. Direct and Play each pass 182 unit tests; both lint tasks and
both debug APK builds pass. Signed release workflow `35118685302` and internal
Google Play draft upload `35119294337` succeeded. GitHub verified the signed
`v0.390` tag. The public APK/AAB SHA-256 values are
`74d1ac4ec060fb06e0783fb2010919973d73c4fca81ed198cf2bec61c0f7603c` and
`9ebc94974d5e9732f5acc9e62d01a56d933c7c6558481616aec1628c90e9e10d`;
downloaded public artifacts match `latest.json`, and the APK reports version
`0.390` (versionCode 390) with the expected signing certificate.

Version `0.389` makes the 30-day battery-level chart use the latest real
reading from each local calendar day instead of plotting every raw telemetry
sample as a dense point cloud. Days without readings remain explicit gaps;
average and range statistics still use every raw sample. Tests cover out-of-
order samples, same-day replacement, and missing-day detection across the
Europe/Berlin daylight-saving transition. All 180 Direct and 180 Play unit
tests, both lint tasks, and both debug APK builds pass locally. Signed release
workflow `35106450436` and internal Google Play draft upload `35107165336`
succeeded. GitHub verified the signed `v0.389` tag. The public APK and AAB
SHA-256 values are `694f52bd9e9f37d7b1643a92f8c34260c959cb20648fca5f467ef7c19c29d827`
and `bf1edb66c8d424d215a9196176e32b063168ec7f35cb3ba053248569dbbaf7d9`;
downloaded public artifacts match `latest.json`, and the APK reports version
`0.389` (versionCode 389).

Version `0.388` makes background-monitor health understandable from Settings:
it reports when the monitor last persisted a heartbeat, flags missing/stale
signals after 30 minutes, and explains that opening Ampere restarts monitoring.
The copy deliberately describes a recent service heartbeat rather than
claiming Android can reliably report a background process as running. Tests
cover recent, missing, future (reboot/clock-domain mismatch), and stale
heartbeats. All 179 Direct and 179 Play unit tests, both lint tasks, and both
debug APK builds pass. The signed release workflow (`35104591888`) succeeded;
the v0.388 SSH tag is verified by GitHub. The public APK/AAB hashes are
`b7b8ae11502be6cbceca8e3fb9df037db0530d90906ca26f72b969997e08672d` and
`2e22b71ebad02ca10deca4597978f163c77789a64f171314c9e1250d89cd253f`.
Downloaded public artifacts match `latest.json`. The internal Play draft upload
(`35105348607`) succeeded.

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

## Recent development changes

Version `0.392` replaces unhelpful unavailable-forecast labels with short,
contextual guidance: while charging without a prior discharge it says “Nach
Entladung”; during a new discharge it indicates the five-minute measurement
threshold or requests more data. The forecast values themselves remain blank
until existing calculation rules have enough evidence.

Version `0.391` makes the first-open guide a compact two-step tour and removes
the blocking analytics-consent dialog from startup; analytics remains opt-in
from Settings. Missing permission/access reminders are limited to once every
30 days, while revocations are still detected immediately. Tapping a history
bar now reveals its exact date, charge, consumption, wear and ratio values;
periods without valid measurement intervals are explicitly identified as
missing data. The independently scaled series are explained in the details.

Version `0.390` adds first-run feature orientation and a live permission audit.
Android notification/channel state, Usage Access AppOp, and overlay state are
read from system APIs on resume. Optional access reminders start only after
the related feature is requested; denied or revoked access can be revisited
from Settings → Berechtigungen prüfen.

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
