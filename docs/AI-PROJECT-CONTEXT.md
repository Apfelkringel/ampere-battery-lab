# AI project context — Ampere Battery Lab

This is a durable, evidence-based map for future agents. It contains repository facts and operational decisions, not runtime user data or private credentials.

## Product scope

Ampere is a local Android battery monitor. It presents battery level, charge state, current, voltage, temperature, power, charging/discharging sessions, health estimates, cycle information, alarms, widget/notification surfaces, optional overlay, and CSV/JSON/TXT exports. Availability depends on OEM and Android APIs; unsupported values remain unavailable.

The iOS port is intentionally narrower: it shows values exposed by iOS and does not pretend to provide Android-only current, capacity, or background monitoring capabilities.

## Source-of-truth hierarchy

1. Runtime behavior and tests in `android/`.
2. `docs/ARCHITECTURE.md`, `docs/VERIFICATION.md`, and `docs/UPDATE-SECURITY.md`.
3. Store/release documentation under `docs/` and `store/`.
4. `README.md` for user-facing scope.
5. Historical commit notes are context, not proof of current behavior.

When sources conflict, inspect current code/tests and document the conflict; do not silently trust an old release note.

## Analytics investigation notes

The code uses Firebase Analytics, not a custom Google Analytics transport. Firebase project configuration is present in `android/app/google-services.json`; actual dashboards and event counts are external and were not available during repository-only analysis.

### Consent flow

1. Startup calls `AnalyticsTracker.restoreConsent()`.
2. No prior grant leaves collection off.
3. Settings exposes the user-facing analytics choice.
4. Granting enables Firebase collection and logs the current section.
5. Withdrawal stores denial, disables collection, and resets analytics data.
6. Battery readings and foreground-app attribution never enter the analytics parameter map.

### Current event inventory

- `ampere_section_view(section=overview|charge|discharge|battery_health|history)`
- `ampere_feature_used(feature=health_measurement|history_export|charge_alarm|discharge_alarm|temperature_alarm|overlay)`

### Questions for an authenticated Firebase/GA read

- How many consenting app instances were active in the last 7/28 days?
- Which sections are viewed most per consenting instance?
- Which allow-listed features are actually used?
- What is the funnel from first section view to health measurement/export?
- Do event volumes differ by app version, Android version, device model, or coarse region, subject to privacy thresholds?
- Are events delayed, missing, or duplicated after consent changes?

Do not infer retention from event totals alone. Separate app-instance counts, event counts, and sessions, and preserve the consent population and retention window in every conclusion.

### Latest verified Analytics snapshot

Source: authenticated Google Analytics/Firebase report for the Ampere property,
read on 2026-09-22. The report range was **25 Aug–21 Sep 2026** (last 28
days), and the report stated that 100% of available data was used for its
cards. This is a snapshot, not a live database export.

- Active users: 69 in 28 days; 69 in 7 days; 1 in the last day.
- Realtime at inspection: 1 active user, country shown as Germany.
- Average engagement time: 1m 56s per active user; 1.4 engaged sessions per
  active user; 1m 17s average engagement per session.
- App stability card: 100.0% users without crashes and 0.0% with crashes.
- Latest app release shown: Ampere Battery Lab `0.468`, status successful.
- MainActivity screen views: 94.
- Revenue: $0.00.

Event report totals for the same range:

| Event | Count | Users | Count per active user |
| --- | ---: | ---: | ---: |
| `ampere_section_view` | 414 | 69 | 6.00 |
| `user_engagement` | 190 | 29 | 6.55 |
| `session_start` | 104 | 69 | 1.51 |
| `screen_view` | 94 | 22 | 4.27 |
| `first_open` | 69 | 69 | 1.00 |
| `ampere_feature_used` | 39 | 18 | 2.17 |
| `app_update` | 22 | 2 | 22.00 |

Interpretation, with the limits of this snapshot: every recorded active user
generated a section-view event, while only 18 users generated a feature event
(about 26% of active users). The six section views per active user suggest
repeated navigation, but do not identify which section without a parameter
breakdown. The 69 first opens and 69 active users indicate a strongly
new-user-heavy 28-day cohort; retention cannot be concluded because the
dashboard snapshot does not provide cohort-day retention. The `app_update`
count is concentrated in only two users and should not be interpreted as 22
distinct upgrades. Investigate event parameters and version dimensions before
changing product decisions.

## Release and distribution facts

- Package/application ID: `com.ampere.batterylab`.
- Last inspected Gradle version: `versionCode 482`, `versionName 0.482`.
- Git HEAD was a `0.483 prep` commit while Gradle still reported 0.482; this is an unresolved release inconsistency until verified.
- Public update manifest URL: `https://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/latest.json`.
- Direct APK and Play bundle are separate distribution paths.
- Private signing is mandatory for release builds.

## Latest Play Console inspection (2026-09-22)

- The dashboard shows green checks for the closed-test release and minimum-12-testers gates. The 14-day closed-test gate is not yet complete, and the Production access action remains disabled.
- The active closed track is `alpha`, covers 177 countries/regions, and currently serves release `0.459`, published 18 Sep 2026. The active test release should not be changed casually while continuous tester eligibility is being accumulated.
- Testers are selected through the Google Group `ampere-battery-lab-testers@googlegroups.com`.
- The closed-track feedback channel was set to the public Reddit test thread: `https://www.reddit.com/r/AndroidAppTesting/comments/1whdrqe/looking_for_12_android_testers_ampere_battery_lab/`.
- The feedback-channel change was submitted on 2026-09-22. Submission activity entry 60 (submitted 17:56 as displayed by Console) reached `Veröffentlicht` at 18:16; the Reddit feedback channel is live.
- Next optimization surfaces to inspect in order: pending publication changes, App content/policy declarations, store listing completeness, quality/statistics, App integrity, and release/version alignment.

### Play Console optimization findings

- Policy status currently reports “Keine Sicherheitslücken gefunden”; App content reports no declarations awaiting review.
- The German default store listing is live under the `Tools` category with four phone screenshots. It has no video, website contact, or selected Play Store tags. The current Growth overview (22 Sep 2026, last 28 days) shows 5 device impressions, 0 device acquisitions, and no available data for first opens, monthly active devices, or 7-day retention. The last-90-days Store-entry card shows 66.67% conversion; this is too sparse to drive a copy change or Store-listing experiment.
- On 2026-09-22 the German default listing was opened through its review step. Play Console displayed no visible blocking validation error. Verified current assets: one app icon, one feature graphic, and four phone screenshots; no video or tablet/Chromebook/XR assets are configured. A free Google Play machine-translation order completed for the Store entry in 28 languages (order `7430adcd1v2`, source en-US, 0.00 USD). The generated locale listings were accepted as drafts after restoring the brand name `Ampere Battery Lab` and enforcing the 80-character short-description limit. The publication overview then showed 31 changes and confirmed that all 31 were moved to `Für später gespeichert`; the queue of changes ready to submit is empty. They remain deferred drafts and are not proof of live publication.
- The closed-test feedback-channel change is complete under published submission 60; no binary, version, or track-release change was part of it.
- Submission 60 detail view confirms source `Play Console` and exactly one change: the Alpha tester feedback channel points to the public Reddit thread. No binary, version, or track-release change is part of this review.
- Play protection recheck (2026-09-22): automatic protection is 1/1 active, Google Play Store protection is 6/7 active, and the displayed 30-day card reports 100.0% Google Play installations with unknown sharing `-`. Play Integrity is 0/7 and explicitly not integrated (the seven signals are therefore unavailable). Adding it requires source/backend integration and a new release, so it must not be toggled casually during the active 14-day closed test. Play Billing protection is 0/4 and is not applicable without Play Billing.
- Device-check option audit (2026-09-22): the missing Store-protection service is Store-listing device checks. It is disabled, and Google requires acceptance of the device-catalog terms before Basic, Device (recommended), or Strict integrity filtering can be selected. No setting was changed because filtering may reduce visibility and tester reach; decide this after the closed-test gate with explicit device-coverage intent.
- Advanced-distribution audit (2026-09-22): app availability is `Veröffentlicht`; Formfaktoren reports Android XR active. Managed Play Store is disabled; Play-as-you-download is disabled with unavailable controls; carrier targeting is off and Production-only; App Actions is unchecked and requires policy/terms acceptance; App indexing has no confirmed websites; Inline installations are unchecked and require the Inline Install API plus eligibility. No setting was changed because these require corresponding app/deep-link/Assistant capabilities.
- Safe next actions: monitor the 14-day gate and Alpha/upload status; later design a Play Integrity integration; then improve store discoverability with evidence-based tags, localization, additional screenshots/video, or a website only when verified assets/content are available.
- Translation quality rule: Google labels the output as machine-generated and not human-reviewed. Spot-check the highest-value locales (German, Spanish, French, Italian, Portuguese, Japanese, Korean, and Chinese) before any combined publication submission; do not blindly submit the mixed asset and localization batch.
- Play Store settings audit (2026-09-22): app type `App`, category `Tools`, no selected tags. Searches for `Batter` and `Akku` returned no relevant available tag, so no tag was added. The listing exposes the developer email but no verified phone or website. External marketing is enabled; no change was made.

### Security and release automation follow-up (2026-09-22)

- Play Console showed an unread notification confirming the intentionally initiated upload-key reset for `com.ampere.batterylab`; the new upload certificate is announced as valid from 24 Sep 2026 15:37 UTC and uploads are blocked until then. Its SHA-1 exactly matches the tracked `android/ampere-upload-certificate.pem`, so this is the expected release-recovery key rather than an unexplained external reset.
- `.github/workflows/publish-google-play.yml` runs every 15 minutes, verifies the public `latest.json`/AAB hash, validates the Play publisher credential, checks the target track through a temporary Play edit, and skips an upload when the version code is already present. Public manifest verified: version `0.487`, versionCode `487`, AAB SHA-256 `394b279b6f4cbc886f0f3c94c85d94f0a2b8b4142f29604585bdfdb2e2b6090c`.
- The 22 Sep validation run after the edit-API fix succeeded; earlier runs failed on the old track endpoint. The workflow was found `disabled_manually` at 18:57 CEST, so a certificate-activation preflight was added and the workflow was re-enabled at 19:00 CEST. A real internal-track attempt then passed the PEM preflight but Google Play rejected the upload with “recently reset”; the Console activation deadline is authoritative. The preflight now honors both the public upload certificate `notBefore` timestamp and `2026-09-24T15:37:00Z`, and skips safely before either condition. The schedule defaults to `internal`, not the active closed-test `alpha` track.
- Live recheck on 2026-09-22: the Dashboard still shows production access disabled because the 14-day closed-test criterion is incomplete; submission 60 is already published and there are no newly submitted changes. The internal test track is shown as inactive. Public manifest version `0.487` must not be described as active on Internal Testing yet; verify the scheduled retry after the Play Console activation deadline has passed.
- Next actions: monitor the 14-day gate and scheduled upload retry, and keep the workflow from targeting `alpha` or `production` without explicit release intent.

### Closed-test recheck (2026-09-22)

- Alpha is still active with release `0.459` (published 18 Sep 2026) in 177 countries/regions. The Tester tab still uses `ampere-battery-lab-testers@googlegroups.com`, the Reddit feedback URL, and the same Android/web participation links.
- The Dashboard's minimum-12-tester criterion remains green, while the Tester tab does not display an exact member or opt-in count. Treat the exact current tester number as unavailable rather than deriving it from group membership or Reddit activity.

### SDK warning remediation (2026-09-22)

- Play Console's 19 Sep notification identifies `androidx.activity:activity:1.0.0` in release `0.459`.
- The current Gradle graph inherited that version transitively through Firebase/Google Play Services. `android/app/build.gradle` now pins stable `androidx.activity:activity:1.13.0`; `dependencyInsight` confirms conflict resolution from `1.0.0` to `1.13.0`.
- Verification passed for both distribution flavors: `testDirectDebugUnitTest`, `lintDirectDebug`, `testPlayDebugUnitTest`, and `lintPlayDebug`.
- The fix is not yet in Google Play; releasing it requires the normal version bump, signed artifact verification, and the closed-test continuity check.

### Android Vitals and size audit (2026-09-22)

- Android Vitals has no available data for user-perceived crashes, ANRs, memory, startup/rendering, battery, or permission-denial metrics. The lost-user metric is explicitly marked as having a limited data basis.
- The App-size page has no representative download/install-size data and no optimization recommendations. Do not interpret these missing values as a clean bill of health; collect more release/user data before making performance claims.

### Manifest and Play-policy audit (2026-09-22)

- `AndroidManifest.xml` targets/compiles against SDK 37, disables cleartext traffic, keeps Firebase Analytics and ad-ID collection disabled by default, and declares no location, contacts, or account permissions.
- The two `specialUse` foreground services carry explicit subtype properties matching local battery telemetry and user-enabled overlay monitoring. Notification, overlay, usage-access, boot, and internet permissions correspond to documented features.
- No new Play App-content declaration was identified from the manifest audit; future permission changes require rechecking the Play forms before release.
- App-content recheck (2026-09-22): the Play Console has no open declarations requiring action and lists 11 completed declarations. Its Data-safety summary reports 3 data types collected or shared, automatic deletion over time, and encryption in transit. This agrees with the current opt-in-only analytics implementation; re-open the declaration before changing SDKs, analytics fields, permissions, or retention.

### Store-copy draft (2026-09-22)

- Saved German short description draft: `Akkuverbrauch, Ladestatus und Verlauf – lokal, transparent, ohne Konto.` (71/80 characters). The wording is supported by the current app/store description.
- Publication overview currently lists three unsubmitted store changes: the new short description plus pre-existing phone-screenshot and feature-graphic drafts. Automatic prechecks completed and the combined submit control is enabled, but no store change was submitted; the pre-existing asset drafts were left untouched.

### Play Statistics snapshot (2026-09-22)

- In the last-28-days App statistics report, the installed-app audience contained only 2 users, observed on 17 and 19 Sep 2026, split one each between Chile and Italy.
- The crash metric displayed “Daten nicht verfügbar”; no crash-rate conclusion is justified. The sample is too small for store-copy, targeting, or stability decisions.

### Ongoing monitoring (2026-09-22)

- A daily Codex thread monitor `Ampere Play-Console-Gate überwachen` is active with failed-run notifications only. It checks submission 60, the 14-day closed-test gate, production access, Alpha/upload status, and the GitHub Play-upload workflow.
- The monitor remains silent when state is unchanged and must record material changes in both `AGENTS.md` and this file. It has no authority to publish to Alpha or Production without an explicit, continuity-safe release action.

## Testing and quality risks

- Android OEM battery files and units vary; central validators must remain the only path into calculations and exports.
- Time changes can move sampling windows backwards; sampling policy must reset its basis rather than fabricate rates.
- Android backup/restore and old telemetry formats need normalization.
- Canvas rendering has a large surface area; preserve real accessibility virtual controls when changing visual controls.
- Check German/English text, small screens, landscape, TalkBack, direct flavor, and Play flavor for user-visible changes.

## Files to read first

- `AGENTS.md` — operating rules and completion gates.
- `README.md` — user-facing feature inventory and development notes.
- `docs/ARCHITECTURE.md` — system boundaries and persistence model.
- `docs/PROJECT-STRUCTURE.md` — repository map and build locations.
- `docs/STORE-PUBLISHING.md` — Play/iOS publishing requirements.
- `docs/UPDATE-SECURITY.md` — update verification and signer policy.
- `android/app/src/main/java/com/ampere/batterylab/AnalyticsTracker.java` — analytics contract.
- `android/app/src/main/java/com/ampere/batterylab/MainActivity.java` — UI/lifecycle integration.
- `android/app/src/test/` — executable behavior and regression coverage.
