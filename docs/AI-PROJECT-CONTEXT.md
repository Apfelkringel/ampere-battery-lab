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
- Current Gradle/public release: `versionCode 488`, `versionName 0.488`. The active Play Alpha remains `0.459` until a continuity-safe Play release decision; the public direct APK/AAB channel is independently at `0.488`.
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
- Play Integrity implementation audit (2026-09-22): the Console's dedicated settings page requires linking a Google Cloud project first; all seven response fields are `Aus`, and its Integrity test control is disabled. The repository contains Firebase Analytics but no Integrity client, nonce/token-verification path, server, Cloud Function, or other backend endpoint. Treat this as a planned post-gate engineering project requiring an ownership/privacy decision and a new signed release, not as a missing Play-Console checkbox.
- Notification audit (2026-09-22): the Play Console notification center was opened and read. It confirms the upload-key reset, repeats the two known deprecated-SDK notices (`androidx.activity:activity:1.0.0` and `androidx.fragment:fragment:1.1.0`) for release `0.459`, and shows the publication, identity-confirmation, and optional Google Ads notices. No new technical blocker was found; no notification was deleted.
- Play Console live recheck (2026-09-23): Dashboard still marks the closed-test release and minimum-12 criteria complete, while the continuous 14-day criterion remains in progress and Production access is disabled. The notification center showed the existing 22 Sep upload-key-reset notice, the two SDK warnings scoped to Alpha `0.459`, the 18 Sep publication message, identity confirmation, and one unread 15 Sep Google Ads offer (€400 credit after €400 spend, new advertisers only). No new technical or policy alert appeared; the Ads offer was opened and no campaign was started.
- Play protection detail recheck (2026-09-22): the automatic-protection report shows 100.0% Google Play installations and no measurable unknown sharing; the Play Store protection panel confirms Play App Signing, Play Protect/SDK scanning, spam/anomaly/fairness protections, and only one missing service: Store-listing device checks. Those checks remain disabled because Google warns they can reduce visibility and require a deliberate device-coverage decision; no change was made during tester acquisition.
- Device-check option audit (2026-09-22): the missing Store-protection service is Store-listing device checks. It is disabled, and Google requires acceptance of the device-catalog terms before Basic, Device (recommended), or Strict integrity filtering can be selected. No setting was changed because filtering may reduce visibility and tester reach; decide this after the closed-test gate with explicit device-coverage intent.
- Advanced-distribution audit (2026-09-22): app availability is `Veröffentlicht`; Formfaktoren reports Android XR active. Managed Play Store is disabled; Play-as-you-download is disabled with unavailable controls; carrier targeting is off and Production-only; App Actions is unchecked and requires policy/terms acceptance; App indexing has no confirmed websites; Inline installations are unchecked and require the Inline Install API plus eligibility. No setting was changed because these require corresponding app/deep-link/Assistant capabilities.
- Policy/program audit (2026-09-22): Richtlinienstatus reports `Keine Sicherheitslücken gefunden`, and App-Inhalte reports `Alles erledigt` with no declarations requiring review. `Apps Experience` is locked until a Production release exists; it is a post-gate opportunity, not an actionable closed-test setting. `Von Pädagogen empfohlen` reports `Nicht zulässig`, so no education-program work should be planned for this app.
- Play App-Optimierung (2026-09-22): the Console setting `App-Optimierung` was enabled and saved after Play's confirmation dialog. It applies to future releases generated from App Bundles and does not retroactively alter existing releases or version drafts. This is a low-risk distribution improvement; the active Alpha release remains unchanged.
- Store metadata cleanup (2026-09-22): Play's default German listing flagged the short description because it used a Halbgeviertstrich (`–`) where the policy check requires a Geviertstrich (`—`). The draft now reads `Batterienutzung, Ladestatus und Verlauf — lokal, transparent, ohne Konto.` (73/80 characters). It was saved and re-opened in the review step, which shows the corrected text; publication was not submitted.
- Verification pass (2026-09-22): `gradle --no-daemon :app:testDirectDebugUnitTest :app:lintDirectDebug :app:testPlayDebugUnitTest :app:lintPlayDebug` completed successfully for both distribution flavors. Only the existing SDK XML version-4 compatibility warning and Gradle deprecation/configuration-cache notices were reported.
- Safe next actions: monitor the 14-day gate and Alpha/upload status; later design a Play Integrity integration; then improve store discoverability with evidence-based tags, localization, additional screenshots/video, or a website only when verified assets/content are available.
- Translation quality rule: Google labels the output as machine-generated and not human-reviewed. Spot-check the highest-value locales (German, Spanish, French, Italian, Portuguese, Japanese, Korean, and Chinese) before any combined publication submission; do not blindly submit the mixed asset and localization batch.
- Translation validation pass (2026-09-22): all 28 generated locale drafts were opened in the actual Store-entry editor and checked against Play's hard 30-character app-name and 80-character short-description limits. Overlong or brand-inconsistent entries were corrected in Spanish (Spain/Latin America), Czech, Hindi, Hungarian, Indonesian, French, Italian, Dutch, Polish, Portuguese (Brazil), Romanian, Russian, Swedish, Thai, Ukrainian, and Vietnamese. Final direct-editor verification is 28/28 valid: every locale uses `Ampere Battery Lab`, every app name is 18 characters, every short description is 29–80 characters, and no editor shows the limit warning. The 31 resulting listing changes were moved back to `Für später gespeichert`; none were submitted or published. Native-language review remains advisable.
- Play Store settings audit (2026-09-22): app type `App`, category `Tools`, no selected tags. Searches for `Batter` and `Akku` returned no relevant available tag, so no tag was added. The listing exposes the developer email but no verified phone or website. External marketing is enabled; no change was made.

### Security and release automation follow-up (2026-09-22)

- Play Console showed an unread notification confirming the intentionally initiated upload-key reset for `com.ampere.batterylab`; the new upload certificate is announced as valid from 24 Sep 2026 15:37 UTC and uploads are blocked until then. Its SHA-1 exactly matches the tracked `android/ampere-upload-certificate.pem`, so this is the expected release-recovery key rather than an unexplained external reset.
- `.github/workflows/publish-google-play.yml` runs every 15 minutes, verifies the public `latest.json`/AAB hash, validates the Play publisher credential, checks the target track through a temporary Play edit, and skips an upload when the version code is already present. Public manifest verified: version `0.488`, versionCode `488`, AAB SHA-256 `6ca8499a35fb3669b7fc505ce45e08370eaf18901b24fd764deb5fd6bf495c37`.
- The 22 Sep validation run after the edit-API fix succeeded; earlier runs failed on the old track endpoint. The workflow was found `disabled_manually` at 18:57 CEST, so a certificate-activation preflight was added and the workflow was re-enabled at 19:00 CEST. A real internal-track attempt then passed the PEM preflight but Google Play rejected the upload with “recently reset”; the Console activation deadline is authoritative. The preflight now honors both the public upload certificate `notBefore` timestamp and `2026-09-24T15:37:00Z`, and skips safely before either condition. The schedule defaults to `internal`, not the active closed-test `alpha` track.
- Upload workflow hardening (2026-09-22): the manual-dispatch version default/example matches verified public manifest `0.488`; a shared concurrency group serializes scheduled/manual runs, and a 15-minute job timeout prevents stale jobs from overlapping future attempts. The default track remains `internal`.
- Upload workflow regression found and fixed (2026-09-23): schedule-triggered runs have no `workflow_dispatch` inputs, so the deployed workflow used fallback version `0.487` and status `completed` while the manual defaults said `0.488` and `draft`. Runs `35826528277` and `35857598086` failed at the initial public-bundle version check (`RELEASE_VERSION: 0.487`); the upload step was never reached and Play state was not mutated. Commit `5be63d9` aligns both runtime fallbacks to version `0.488` and status `draft`. Validation-only run `35867880435` on that commit succeeded: public bundle/hash and Play API access passed, the target remained `internal`, and upload was safely skipped by the activation preflight through `2026-09-24T15:37:00Z`.
- Live recheck on 2026-09-22: the Dashboard still shows production access disabled because the 14-day closed-test criterion is incomplete; submission 60 is already published and there are no newly submitted changes. The internal test track is shown as inactive. Public manifest version `0.488` must not be described as active on Internal Testing yet; verify the scheduled retry after the Play Console activation deadline has passed.
- Internal-track detail recheck (2026-09-23): Play Console confirms Internal Testing is `Inaktiv`, with a saved unpublished version draft `0.384` (version code `467`, bundle explorer artifact `4860231764308114663`, support for 20,319 Android devices). Draft setup is 1/3 complete; there is no active internal release. This legacy draft is not the public `0.488` bundle and was left unchanged. Workflow inspection: validation-only run `35867880435` succeeded; scheduled runs `35826528277` and `35857598086` failed before the runtime-default fix, and no post-fix scheduled run is listed yet. Continue monitoring; no schedule/track mutation was made.
- Play growth and gate recheck (2026-09-23): Dashboard still confirms the release and minimum-12 closed-test checks, while the continuous 14-day criterion remains incomplete and Production access disabled; update status is “Noch nicht zur Überprüfung gesendet” and Production is inactive. Growth overview (last 28 days) shows 6 device impressions, 2 device acquisitions, and 2 first opens (0% delta); monthly active devices and 7-day retention are unavailable. The separate last-90-days Store-entry card shows 66.67% conversion. Store Listings' 28-day detail report for `2026-08-23`–`2026-09-19` displays 3 visitors, 2 single-user install clicks, and 67% click-through rate; the standard-listing row separately displays 1 visitor and conversion `-`. Do not blend these different periods/surfaces into one conversion funnel without a verified common basis. App Statistics for its last-28-days range through `2026-09-22` lists 2 installed-app users, on 17 and 19 Sep, from Chile and Italy; this is not the Play closed-test opt-in count. Store Listing experiments remain 0 running/0 completed/0 applied. The notification center contains only the known upload-key-reset/SDK/publication/identity/Ads items and no new technical alert. No scheduled GitHub run after the successful validation-only run `35867880435` is currently listed. Given this sparse traffic, no experiment, speculative targeting, or deferred translation/listing publication is justified.
- Store-change queue safeguard (2026-09-23): despite the earlier verified state of 31 changes in `Für später gespeichert` with an empty review queue, the live Publishing overview showed 31 Store-entry changes ready for submission. None was submitted. The explicit `Für später speichern` action was applied to the Store-entry group; Console confirmed `31 Änderungen für später gespeichert` and the review queue returned to its empty-state message. All machine-translation/listing changes remain deferred pending quality/provenance review; the live listing was not changed.
- Feedback and ratings recheck (2026-09-23): Play Console's Testfeedback view contained only explanatory empty-state text and no tester submission. The Ratings report (last 28 days) showed 0 users and no reviews matching the filters, so there is nothing to answer. This is not evidence of zero crashes or positive user sentiment; Android Vitals remains unavailable. Keep encouraging candid private testing feedback via the existing route, never soliciting positive ratings.
- Policy/program recheck (2026-09-23): the live Policy status reports `Keine Sicherheitslücken gefunden`, and App content → `Überprüfung erforderlich` reports `Alles erledigt` with no pending declaration. Apps Experience is explicitly `Gesperrt` until a Production release exists. No declarations or policy settings were changed; Production remains unavailable until the closed-test 14-day criterion is satisfied.
- Google Play protection recheck (2026-09-23): automatic protection remains 1/1 active; Google Play Store protection is 6/7; Play Integrity is 0/7 and not integrated; Play Billing protection is 0/4. The sole inactive Store-protection service is `Geräteprüfungen für Store-Eintrag`; other displayed services (Play App Signing, Play Protect/SDK scans, spam/anomaly/fairness protections) remain active. The 30-day card shows 100.0% Google Play installations and unknown sharing `-`. Device checks remain off because enabling them requires separate device-catalog terms acceptance and may reduce eligible device/virtual-device reach during tester recruitment. No settings or terms were changed.
- Post-release validation (2026-09-22): manual validation run `35765901274` downloaded public `0.488`, verified its AAB hash, authenticated the Play publisher with read-only API access, and safely skipped upload because the Console activation deadline `2026-09-24T15:37:00Z` has not passed. No Play state was mutated. A fresh Dashboard recheck still shows Production inactive, Closed testing active with one track, Internal testing inactive, all release/minimum-12 checks green, and the 14-day criterion incomplete.
- Pre-Launch audit (2026-09-22): Play's Pre-Launch-Bericht page reports `Artefakte hochladen, um Pre-Launch-Berichte zu erstellen` and has no report to inspect. The technical-quality cards in the Testen-und-veröffentlichen overview are still attached to Alpha `0.459`; they are not evidence about the unreleased/public `0.488`. No extra track was created; after the upload-key window, use the planned internal `0.488` upload and inspect the resulting report before any Play release decision.
- Play Console recheck (2026-09-22): Dashboard status is still “Store updates not submitted”; Alpha is the only active test track, Internal Testing is inactive, and Production access remains disabled. The unread reset notification still states the authoritative upload-key activation time `2026-09-24 15:37 UTC`; no newer blocking warning appeared. Because Alpha `0.459` was published 18 Sep 2026, the earliest 14-day checkpoint is approximately 02 Oct 2026; this is a planning date, not proof that the gate has completed.
- Optimization audit (2026-09-22): the Play Console optimization area was reviewed. Device Catalog access is gated by a separate terms-of-service dialog, which was left untouched; Testfeedback has no visible entries; Android Vitals still has no actionable crash/ANR or app-size data; policy status reports `Keine Sicherheitslücken gefunden`; Apps Experience is locked until a production release.
- Store-discovery audit (2026-09-22): the live default listing reports 1 visitor and zero running or completed Store Listing tests. The app remains in category `Tools`; Play's tag manager returned no matching tag for `Batterie` or `Monitor`, so no speculative tag was selected. The developer email is present, while phone and website are empty; no unverified contact data was invented. External marketing is enabled.
- Translation audit (2026-09-22): Google Play machine-translation order `7430adcd1v2` remains completed for 28 languages at `0,00 USD`. The rows still offer `Prüfen und übernehmen`; the generated listing changes remain deferred in `Für später gespeichert` and must receive a deliberate review before submission.
- Play Console live recheck (2026-09-23): the Dashboard still marks the closed-test release and minimum-12 criteria complete, while the continuous 14-day criterion remains in progress and Production access is disabled. The notification center showed the existing 22 Sep upload-key-reset notice, both SDK warnings scoped to Alpha `0.459`, the 18 Sep publication message, identity confirmation, and one unread 15 Sep Google Ads offer (€400 credit after €400 spend, new advertisers only). No new technical or policy alert appeared; the offer was opened and no campaign was started.
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
- Technical-quality remediation (2026-09-22): Play's next-release panel flagged transitive `androidx.fragment:fragment:1.1.0` and Android 15 edge-to-edge APIs in Alpha `0.459`. The source now pins stable Fragment `1.9.0`; `dependencyInsight` resolves the old `1.1.0`/`1.0.0` requests to `1.9.0`. `MainActivity` no longer calls Android 15-disabled `setStatusBarColor`, `setNavigationBarColor`, or `setDecorFitsSystemWindows`; its API-35 `WindowInsets` padding remains, and the theme uses a window background instead of deprecated bar-color resources. Direct/Play unit tests, lint, and debug assemblies pass. Signed release `0.488` contains the fix on the public direct/AAB channel; it remains unreleased to Play Alpha pending the continuity-safe release decision.
- Release verification (2026-09-22): signed tag `v0.488` and GitHub Actions run `35764669114` passed metadata validation, Direct/Play tests and lint, signed APK/AAB builds, certificate-lineage verification, provenance attestations, and release attachment. Public update repository commits `20bff3b`, `0ce7ec2`, `86ceb4f`, and `6b74a7a` publish the APK, AAB, `latest.json`, and checksums. Fresh public downloads verified package `com.ampere.batterylab`, version code/name `488`/`0.488`, APK SHA-256 `c2acbfcbf86d40481d5698ba8add3cdbe2d806ef5987d9cc6c2fc0c4f798fec4`, and AAB SHA-256 `6ca8499a35fb3669b7fc505ce45e08370eaf18901b24fd764deb5fd6bf495c37` against `latest.json`.
- Growth/AI audit (2026-09-22): the Play growth overview showed 5 device impressions in the last 28 days, 1 Store-listing visitor, and a displayed 66.67% Store conversion rate; device acquisitions, first opens, monthly active devices, and 7-day retention were unavailable. The built-in Play AI analysis of perceived crash rate by Android version only confirmed coverage of Android 14, 15, and 16 and had no data-backed recommendation because the breakdown was unavailable. The sample is too small for a Store Listing experiment or targeting decision; the experiment setup was inspected but no test was created and no saved listing/translation changes were submitted.

### Manifest and Play-policy audit (2026-09-22)

- `AndroidManifest.xml` targets/compiles against SDK 37, disables cleartext traffic, keeps Firebase Analytics and ad-ID collection disabled by default, and declares no location, contacts, or account permissions.
- The two `specialUse` foreground services carry explicit subtype properties matching local battery telemetry and user-enabled overlay monitoring. Notification, overlay, usage-access, boot, and internet permissions correspond to documented features.
- No new Play App-content declaration was identified from the manifest audit; future permission changes require rechecking the Play forms before release.
- App-content recheck (2026-09-22): the Play Console has no open declarations requiring action and lists 11 completed declarations. Its Data-safety summary reports 3 data types collected or shared, automatic deletion over time, and encryption in transit. This agrees with the current opt-in-only analytics implementation; re-open the declaration before changing SDKs, analytics fields, permissions, or retention.

### Store-copy draft (2026-09-22)

- Saved German short description draft: `Batterienutzung, Ladestatus und Verlauf — lokal, transparent, ohne Konto.` (73/80 characters). This exact text is also synchronized to `store/google-play/listing/short-description.txt`; the Play editor accepted it without the dash-policy warning.
- Publication overview currently holds 31 unsubmitted Store-entry changes (28 locale additions plus German text/media changes) under `Für später gespeichert`. None were submitted; the pre-existing phone-screenshot and feature-graphic drafts remain untouched.
- A local regression guard at `tooling/validate-google-play-metadata.sh` checks the synchronized `app-name.txt` and German short-description source for Play's 1–30/1–80 character limits and dash policy; `.github/workflows/build-apk.yml` runs it before Android tests and release builds.
- `.github/workflows/validate-play-metadata.yml` now runs the same guard on relevant pushes, pull requests, and manual dispatches, providing a fast metadata-only gate before a full signed build is needed.
- Fast metadata CI verification (2026-09-22): push run `35761077182` completed successfully for commit `a36bdc7`.
- App-name guard verification (2026-09-22): push run `35761445345` completed successfully for commit `14a3909`, validating app name `18/30` and short description `73/80`.
- CI verification (2026-09-22): GitHub Actions run `35760526133` passed the metadata guard (`73/80`), direct tests/lint, signed direct APK and Play App Bundle builds, certificate-lineage verification, provenance attestations, and artifact upload. The run completed successfully; GitHub's Node.js/runner migration notices and existing APK/JAR metadata signature warnings were non-blocking.
- CI action maintenance (2026-09-22): updated pinned Actions across Android, iOS, TestFlight, and metadata workflows to verified current releases: checkout `v7.0.1`, setup-java `v6.0.1`, setup-gradle `v6.3.0`, setup-android `v4.0.4`, upload-artifact `v7.0.1`, and attest-build-provenance `v4.2.2`. The Android workflow must be re-run to verify compatibility.
- CI action migration verification (2026-09-22): Android build run `35761765426` passed metadata validation, tests/lint, signed APK/AAB builds, certificate-lineage checks, attestations, and artifact upload with the new pins. The prior Node.js 20 action warnings disappeared; only the independent future `ubuntu-latest` runner migration notice remains.
- Runner stability (2026-09-22): Linux workflows now pin `runs-on: ubuntu-24.04` for Android builds, Play publishing, and metadata validation, avoiding the announced automatic Ubuntu 26 migration. iOS workflows retain their explicit `xcode-27` runner.
- Runner verification (2026-09-22): full Android build run `35762509814` succeeded on the pinned Ubuntu 24.04 runner for commit `7e48bda`. Metadata validation, unit tests/lint, signed APK/AAB builds, certificate-lineage verification, provenance attestations, and artifact upload all passed; tagged-release APK attachment was skipped as expected because the dispatch was from `main`, not a tag. The Ubuntu migration notice is gone; remaining warnings are limited to SDK-manager, Gradle, and JAR-signature metadata details.

### Play Statistics snapshot (2026-09-22)

- In the last-28-days App statistics report, the installed-app audience contained only 2 users, observed on 17 and 19 Sep 2026, split one each between Chile and Italy.
- The crash metric displayed “Daten nicht verfügbar”; no crash-rate conclusion is justified. The sample is too small for store-copy, targeting, or stability decisions.

### Ongoing monitoring (2026-09-22)

- A twice-daily Codex thread monitor `Ampere Play-Console-Gate überwachen` is active with failed-run notifications only (`FREQ=DAILY;BYHOUR=9,18;BYMINUTE=0`, host/local Europe/Berlin schedule). It checks submission 60, the 14-day closed-test gate, production access, Alpha/upload status, the GitHub Play-upload workflow, unread Play notifications, growth metrics, Store Listing tests, translation/listing drafts, Store-Tag/device-catalog changes, and Play-AI/Vitals results. It must not start an experiment on a statistically tiny sample.
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
