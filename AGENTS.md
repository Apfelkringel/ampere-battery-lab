# Ampere Battery Lab — AI project instructions

Read this file before changing code, release metadata, analytics, store text, or tester-recruitment material. The detailed project map is in `docs/AI-PROJECT-CONTEXT.md`.

## Project identity

- Product: Ampere Battery Lab, an Android-first local battery-analysis app.
- Android package and iOS bundle ID: `com.ampere.batterylab`.
- Android source: `android/`; iOS port: `ios/`; web prototype: `prototypes/web/`.
- Battery readings, history, sessions, and telemetry remain on-device. There is no Ampere application server.
- Public binary/update repository: `https://github.com/Apfelkringel/ampere-battery-lab-updates`.

## Safety and privacy rules

- Never commit signing keys, passwords, service-account JSON, API secrets, personal tester email addresses, or private analytics exports.
- Do not add analytics for battery values, foreground-app names, precise location, account identifiers, or free-form user content.
- Analytics is opt-in only. Preserve default-off manifest flags, consent storage, Firebase consent, and reset-on-withdrawal behavior.
- Never claim live Google Analytics numbers without an authenticated source, query period, filters, and freshness.

## Current verified implementation

- Android compile/target SDK: 37; min SDK: 23.
- Distribution flavors: `direct` and `play`; direct may use the signed GitHub APK update flow, while Play relies on Google Play updates.
- Firebase Analytics uses Firebase BoM `34.19.0`; `android/app/google-services.json` is public project configuration, not a secret or Console access.
- Gradle currently contains `versionCode 482` and `versionName "0.482"`. Git HEAD is a `0.483 prep` commit, so version state must be reconciled before any release claim.

## Analytics contract

Implementation: `android/app/src/main/java/com/ampere/batterylab/AnalyticsTracker.java`.

Consent is stored separately in `ampere-privacy`, outside exported/restored battery preferences. Collection is disabled until enabled by the user; disabling calls `resetAnalyticsData()`.

| Event | Parameters | Meaning |
| --- | --- | --- |
| `ampere_section_view` | `section`: `overview`, `charge`, `discharge`, `battery_health`, `history` | A section opened after consent. |
| `ampere_feature_used` | `feature`: `health_measurement`, `history_export`, `charge_alarm`, `discharge_alarm`, `temperature_alarm`, `overlay` | An allow-listed feature activated after consent. |

Automatic screen reporting is disabled. Ad storage, ad user data, and ad personalization are explicitly denied. Do not broaden this vocabulary without updating privacy text, Play Data Safety, tests, and this document.

Repository inspection establishes schema and privacy boundaries, but not active users, retention, conversion, or event counts. For those metrics, use the authenticated Firebase/Google Analytics property and record property/stream, UTC date range, timezone, consent population, filters, freshness, and sampling/modeling status. If unavailable, report the limitation rather than guessing or substituting Reddit activity.

## Architecture map

- `MainActivity` is the lifecycle/UI adapter and canvas-style dashboard renderer.
- `BatteryDataRepository` is the persistence seam; current storage uses `SharedPreferences`.
- `BatterySamplingPolicy` owns sampling interval, rollback handling, and about 30-day telemetry retention.
- `BatteryMonitorService` collects validated snapshots and system events on a background thread.
- `BatteryBackupCodec` owns backup/restore allowlists and type encoding.
- `BatteryTelemetryDiagnostics`, `BatteryExportRules`, and `Battery*Rules` centralize validation and deterministic exports.
- Read `docs/ARCHITECTURE.md` and `docs/PROJECT-STRUCTURE.md` when boundaries change.

## Verification commands

Use Java 17 and explicit flavor/task names from `android/`:

```sh
gradle --no-daemon :app:testDirectDebugUnitTest :app:lintDirectDebug :app:assembleDirectDebug
gradle --no-daemon :app:testPlayDebugUnitTest :app:lintPlayDebug :app:assemblePlayDebug
```

Release tasks require private signing credentials and must fail rather than fall back to a debug key. Analytics changes need tests for consent persistence, allow-listed events, denial/reset behavior, and parameter boundaries.

## Release completion rule

User-visible Ampere changes are not complete when source code is merely pushed. Unless the user explicitly says otherwise, finish by:

1. incrementing Android app version code/name;
2. running relevant tests and lint;
3. publishing the signed, tagged release;
4. updating the public APK and `latest.json` in `ampere-battery-lab-updates`;
5. verifying public APK package/version and SHA-256 against `latest.json`.

Follow `docs/STORE-PUBLISHING.md`, `docs/UPDATE-SECURITY.md`, and `tooling/release-ampere/README.md`. Never expose signing material in logs.

## Working conventions

- Preserve unrelated user changes and inspect `git status` before editing.
- Prefer small, testable Java rule classes over adding logic to the renderer.
- Keep unavailable measurements explicitly unavailable; never invent zeroes.
- Preserve German and English strings together through `AppText`.
- Check accessibility, narrow layouts, landscape, backup/restore, and both distribution flavors for UI or data-flow changes.
- Record new durable facts in `docs/AI-PROJECT-CONTEXT.md` and update this file when an instruction or invariant changes.

## Tester, Reddit, and document maintenance

- The current closed-test objective is to maintain at least 12 continuously opted-in Google Play testers for the required 14-day period before requesting production access. Treat the Play Console dashboard as authoritative; Reddit replies, group membership, and chat messages are only supporting evidence.
- Whenever Reddit is opened for tester recruitment, first inspect notifications, red markers, inbox messages, and chat requests. Read the full message before responding. Accept relevant tester/test-for-test chat requests, keep coordination public where practical, and never request or publish tester email addresses.
- For each tester who joins, send the official Ampere group, opt-in, and install links; ask the tester to remain opted in and keep the app installed for the full period. Do not promise ratings or positive reviews.
- Before claiming tester-goal completion, re-check the Play Console count/status and record whether the 14-day criterion is still running or complete. Never infer the count from Reddit activity alone.
- Keep the tester-link document current and ordered per app as: opt-in/test group, then install link, then any internal-test caveat. Current document: `https://docs.google.com/document/d/10eYlUuuS6M93f26NxNXU4eu3q64PyqyCS5NyzWfYx0I/edit`.
- `AGENTS.md` is not updated by a background process. At the start of each substantive project session, inspect it and `docs/AI-PROJECT-CONTEXT.md`; after any durable change to project facts, tester workflow, analytics/privacy rules, release process, or document links, update both files in the same change.
- A daily Codex thread monitor named `Ampere Play-Console-Gate überwachen` is active. It checks submission 60, the 14-day gate, production access, Alpha/upload status, the GitHub upload workflow, unread Play notifications, growth metrics, Store Listing tests, translation/listing drafts, and Play-AI/Vitals results; it stays quiet when unchanged and must update both project context files when a material change occurs. It must not start an experiment on a statistically tiny sample and does not authorize Alpha/Production releases.
- Before handoff, verify that these instructions still match the current Play Console status, public update metadata, analytics implementation, and tester document. State any unavailable or stale external information explicitly instead of guessing.

### Latest Play Console inspection (2026-09-22)

- Dashboard: the closed-test release criterion and the minimum-12-testers criterion show green checks; the 14-day closed-test criterion is still in progress and Production access remains disabled.
- Closed track: `alpha`, active, 177 countries/regions, newest track release `0.459` published 18 Sep 2026. Do not replace this release during the active 14-day window without checking the effect on continuous tester eligibility.
- Tester selection: Google Group `ampere-battery-lab-testers@googlegroups.com`; Android participation and web opt-in links are visible in the track's Tester tab.
- Optimization applied: the closed track feedback channel was set to the public Reddit test thread `https://www.reddit.com/r/AndroidAppTesting/comments/1whdrqe/looking_for_12_android_testers_ampere_battery_lab/`.
- The feedback-channel change was submitted on 2026-09-22; submission activity entry 60 was published by Play Console at 18:16 (submitted 17:56 as displayed by Console). Treat the Reddit feedback channel as live.
- Submission 60 details were re-opened: source is Play Console and exactly one change is listed—changing the Alpha tester feedback channel to the public Reddit thread. No binary, version, or track-release change was included.
- Future Play Console review order: Dashboard gates → closed-track release/tester settings → pending publication changes → app content/policy declarations → store listing → statistics/quality → App integrity. Record each concrete finding here and in `docs/AI-PROJECT-CONTEXT.md`.
- Additional inspection: Policy status reports “Keine Sicherheitslücken gefunden”; App content has no declarations awaiting review.
- Store listing is live in German with category `Tools`, four phone screenshots, no video, no website contact, and no selected Play Store tags. The current Growth overview (22 Sep 2026, last 28 days) shows 5 device impressions, 0% delta, and no available device-acquisition/first-open/MAU/7-day-retention data; the last-90-days Store-entry card shows 66.67% conversion. Treat the conversion as an extremely sparse signal, not a stable KPI. Do not start a Store-listing experiment or invent tags/copy until traffic is materially larger.
- Store-listing validation was rechecked on 2026-09-22: the German default listing reached the review step without a visible blocking validation error. Current assets are one icon, one feature graphic, and four phone screenshots; no video or tablet/Chromebook/XR assets are configured. A free Google Play machine-translation order was completed on 2026-09-22 for 28 languages (order `7430adcd1v2`, 0.00 USD, source en-US, Store entry only). The generated listings were reviewed in the Console and accepted as drafts; the brand name was normalized back to `Ampere Battery Lab` and overlong machine-generated short descriptions were shortened to the 80-character limit. The publication overview then showed 31 changes (28 locale additions plus existing German copy/media drafts); all 31 were moved to `Für später gespeichert`, leaving the review-submission queue empty. They are not live publication and must remain deferred until provenance and language quality are checked.
- Play protection recheck (2026-09-22): automatic protection is 1/1 active; Google Play Store protection is 6/7 active; and Google Play installations are 100.0% in the displayed 30-day card, with unknown sharing shown as `-`. Play Integrity is 0/7 active and explicitly not integrated; enabling it requires an SDK/backend integration and a new release, so it is a planned engineering item, not a console toggle to change during the active closed test. Play Billing protection is 0/4 and is not relevant unless the app adds Play Billing.
- Play protection detail recheck (2026-09-22): the automatic-protection report shows 100.0% Google Play installations and no measurable unknown sharing; the Play Store protection panel confirms Play App Signing, Play Protect/SDK scanning, spam/anomaly/fairness protections, and only one missing service: Store-listing device checks. Those checks remain disabled because Google warns they can reduce visibility and require a deliberate device-coverage decision; do not enable them during tester acquisition.
- Device-check option audit (2026-09-22): the missing Store-protection service is `Geräteprüfungen für Store-Eintrag`; it is disabled and Google requires acceptance of the device-catalog terms before enabling Basic, Device (recommended), or Strict integrity filtering. Do not enable it automatically: it can reduce visibility for devices/virtual environments and could affect tester reach. Revisit after the closed-test gate with an explicit device-coverage decision.
- Advanced-distribution audit (2026-09-22): app availability is `Veröffentlicht`; the Formfaktoren page reports Android XR as active. Managed Play Store is disabled; Play-as-you-download is disabled and its controls are unavailable; carrier targeting is off and applies only to Production; App Actions is unchecked and requires policy/terms acceptance; App indexing has no confirmed websites; Inline installations are unchecked and require the Inline Install API plus eligibility. Do not enable these features without matching app/deep-link/Assistant implementations and verified product intent.
- Policy/program audit (2026-09-22): Richtlinienstatus reports `Keine Sicherheitslücken gefunden`, and App-Inhalte reports `Alles erledigt` with no declarations requiring review. `Apps Experience` is locked until a Production release exists; it is a post-gate opportunity, not an actionable closed-test setting. `Von Pädagogen empfohlen` reports `Nicht zulässig`, so no education-program work should be planned for this app.
- Play App-Optimierung (2026-09-22): Google Play Console's `App-Optimierung` was enabled and confirmed saved. Google Play will optimize APKs generated from future App Bundles for performance, size, and security; Play explicitly states that existing releases and version drafts are not retroactively changed. This is a safe distribution optimization and does not alter the active Alpha release.
- Store metadata cleanup (2026-09-22): Play's default German listing flagged the short description because it used a Halbgeviertstrich (`–`) where the policy check requires a Geviertstrich (`—`). The draft was corrected to `Batterienutzung, Ladestatus und Verlauf — lokal, transparent, ohne Konto.` (73/80 characters), saved, and the review step now shows the corrected text without that warning. It remains an unsubmitted draft; no publication was triggered.
- Metadata source sync (2026-09-22): `store/google-play/listing/short-description.txt` now contains the exact 73-character German Play draft, including the required Geviertstrich. Keep this source file and the Play default listing synchronized before future Store-entry submissions.
- Metadata regression guard (2026-09-22): `tooling/validate-google-play-metadata.sh` now enforces Play's 1–30 app-name and 1–80 short-description limits, requires the Play-approved em dash, rejects en-dash/double-hyphen variants, and runs before Android tests in `.github/workflows/build-apk.yml`. Run it locally before changing Store copy.
- Fast metadata CI (2026-09-22): `.github/workflows/validate-play-metadata.yml` runs the same guard on relevant pushes, pull requests, and manual dispatches, so listing regressions are caught without waiting for a signed Android build.
- Metadata source sync (2026-09-22): `store/google-play/listing/app-name.txt` now records the verified 18-character name `Ampere Battery Lab`; the local guard validates it against Play's 30-character limit.
- Fast metadata CI verification (2026-09-22): push run `35761077182` completed successfully on commit `a36bdc7`.
- App-name guard verification (2026-09-22): push run `35761445345` completed successfully on commit `14a3909`; it validated app name `18/30` and short description `73/80`.
- CI verification (2026-09-22): GitHub Actions run `35760526133` passed the new metadata guard (`73/80`), direct unit tests/lint, signed direct APK build, Play App Bundle build, certificate-lineage verification, provenance attestations, and artifact upload. GitHub emitted only its existing Node.js 20/ubuntu-latest migration notices and APK/JAR metadata signature warnings; the job completed successfully.
- CI action maintenance (2026-09-22): updated the pinned GitHub Actions used by Android, iOS, TestFlight, and metadata workflows to the verified current releases: checkout `v7.0.1`, setup-java `v6.0.1`, setup-gradle `v6.3.0`, setup-android `v4.0.4`, upload-artifact `v7.0.1`, and attest-build-provenance `v4.2.2`. Re-run the Android build before treating the migration as complete.
- CI action migration verification (2026-09-22): Android build run `35761765426` passed metadata validation, tests/lint, signed APK/AAB builds, certificate-lineage checks, attestations, and artifact upload with the new pins. The prior Node.js 20 action warnings disappeared; only the independent future `ubuntu-latest` runner migration notice remains.
- Runner stability (2026-09-22): Linux workflows now pin `runs-on: ubuntu-24.04` for Android builds, Play publishing, and metadata validation. This prevents the announced automatic `ubuntu-latest` migration to Ubuntu 26 from changing release behavior unexpectedly; iOS workflows keep their explicit `xcode-27` runner.
- Runner verification (2026-09-22): full Android build run `35762509814` succeeded on the pinned Ubuntu 24.04 runner for commit `7e48bda`. Metadata validation, unit tests/lint, signed APK/AAB builds, certificate-lineage verification, provenance attestations, and artifact upload all passed; release attachment was skipped as expected because the dispatch was from `main`, not a tag. The prior Ubuntu migration notice is no longer present; remaining warnings are SDK-manager, Gradle, and JAR-signature metadata warnings only.
- Verification pass (2026-09-22): `gradle --no-daemon :app:testDirectDebugUnitTest :app:lintDirectDebug :app:testPlayDebugUnitTest :app:lintPlayDebug` completed successfully. Gradle reported only the existing SDK XML version-4 compatibility warning and deprecation/configuration-cache notices; no test or lint failure occurred.
- Security/release alert: Play Console showed an unread 22 Sep 2026 notification that the intentionally initiated upload-key reset for `com.ampere.batterylab` was accepted. The new upload certificate is announced as valid from 24 Sep 2026 15:37 UTC and uploads are blocked until then. Its SHA-1 matches the tracked `android/ampere-upload-certificate.pem` exactly; do not request a second reset or expose private key material.
- Upload evidence: a real internal-track attempt on 22 Sep passed the certificate-file preflight but Google Play rejected it with “The upload certificate this APK is signed with is not yet valid because it has been recently reset.” The Console activation time is authoritative over the certificate PEM `notBefore` timestamp; do not retry before 24 Sep 2026 15:37 UTC.
- Release automation inspection: `.github/workflows/publish-google-play.yml` is active on GitHub and runs every 15 minutes, verifies the public `latest.json`/AAB hash, validates the Play publisher credential, checks the target track through a temporary Play edit, and skips an upload when the version code is already present. Its certificate preflight now honors both the tracked public certificate `notBefore` timestamp and the Play Console activation deadline `2026-09-24T15:37:00Z`, preventing repeated “recently reset” failures. The latest verified public manifest is version `0.487`/code `487`; validation succeeded after the edit-API fix, while the real internal upload was correctly blocked by Google Play’s reset window. The schedule defaults to the `internal` track and must not be changed to `alpha` or `production` without explicit release intent.
- Upload workflow hardening (2026-09-22): `.github/workflows/publish-google-play.yml` now uses `0.487` as the manual-dispatch example/default, matching the verified public manifest, serializes scheduled/manual runs with a shared concurrency group, and applies a 15-minute job timeout. This prevents stale manual defaults and overlapping upload attempts while preserving the safe `internal` default track.
- Current live gate recheck: Dashboard still shows the production-access button disabled because the 14-day closed-test criterion is incomplete. Submission 60 is published; the Dashboard has no newly submitted changes. The internal test track is currently shown as inactive and `0.487` is not yet verified there. Wait until the Console activation deadline has passed, then verify the scheduled internal upload and track state.
- Play Console recheck (2026-09-22): the Dashboard still reports Store updates as not submitted, Alpha as the only active test track, Internal Testing as inactive, and Production access as disabled. The unread upload-key notification repeats the authoritative activation time `2026-09-24 15:37 UTC`; no newer blocking warning appeared. Alpha `0.459` was published 18 Sep 2026, so the earliest 14-day closed-test checkpoint is approximately 02 Oct 2026; do not request Production access before Play marks the gate complete.
- Closed-test recheck (2026-09-22): Alpha remains active with release `0.459` (published 18 Sep 2026) in 177 countries/regions. The Tester tab still selects `ampere-battery-lab-testers@googlegroups.com`, keeps the Reddit feedback URL, and shows the same Android/web participation links. The Dashboard's minimum-12-tester check remains green, but the Tester tab exposes no exact member/opt-in count; do not infer a numeric tester count from the group address or Reddit activity.
- SDK warning follow-up: Play Console's 19 Sep warning identifies `androidx.activity:activity:1.0.0` in release `0.459`. The current Gradle graph also inherited that version transitively through Firebase/Google Play Services, so `android/app/build.gradle` now pins stable `androidx.activity:activity:1.13.0`. `dependencyInsight` confirms `1.0.0 -> 1.13.0`; `testDirectDebugUnitTest`, `lintDirectDebug`, `testPlayDebugUnitTest`, and `lintPlayDebug` all pass. This is source-only until a deliberately versioned release is built and published.
- Manifest/Play-policy audit (2026-09-22): target/compile SDK are 37; analytics collection and ad-ID signals are default-disabled; cleartext traffic is disabled; no location/contact/account permissions are declared; both special-use foreground services have explicit user-facing subtype properties; notifications, overlay, usage access, boot, and internet permissions match documented features. No new Play declaration should be added without checking the App content forms.
- App-content recheck (2026-09-22): Play Console shows no declarations requiring action and 11 completed declarations. The completed Data-safety summary reports 3 data types collected or shared, automatic deletion over time, and encryption in transit. This remains consistent with the current opt-in-only analytics allow-list and must be revisited if analytics fields, SDKs, permissions, or retention change.
- Play Statistics snapshot (2026-09-22): the last-28-days App statistics report showed only 2 installed-app users, on 17 and 19 Sep, split between Chile and Italy. The crash metric returned “Daten nicht verfügbar”; this is insufficient to infer stability or market fit. Keep decisions conservative until more data accumulates.
- Android Vitals recheck (2026-09-22): Play Console reports no data for user-perceived crashes, ANRs, memory, startup/rendering, battery, or permission-denial metrics; the key lost-user metric is marked “Begrenzte Datenbasis”. The App-size page likewise has no representative download-size or optimization data and no recommendations. Treat this as insufficient sample/release coverage, not as proof of zero crashes or optimal size.
- Growth/AI audit (2026-09-22): the Play growth overview showed 5 device impressions in the last 28 days, 1 Store-listing visitor, and a displayed 66.67% Store conversion rate; device acquisitions, first opens, monthly active devices, and 7-day retention were unavailable. The built-in Play AI analysis of perceived crash rate by Android version only confirmed coverage of Android 14, 15, and 16 and had no data-backed recommendation because the breakdown was unavailable. The sample is too small for a Store Listing experiment or targeting decision; the experiment setup was inspected but no test was created and no saved listing/translation changes were submitted.
- Historical store-copy state (superseded 2026-09-22): an earlier draft used `Akkuverbrauch, Ladestatus und Verlauf – lokal, transparent, ohne Konto.` (71/80 characters) and briefly showed three pending changes. This was replaced by the current synchronized 73-character German draft and the later 31-change machine-translation/listing batch documented below; do not use this historical snapshot as the current Play state.
- Machine translation caveat: Google explicitly states these translations are machine-generated and not human-reviewed. Keep the 28 locale drafts separate from the live listing until each important market is spot-checked; do not claim that Google has published them merely because the order is “Abgeschlossen”.
- Translation validation pass (2026-09-22): all 28 generated locale drafts were opened in the actual Store-entry editor and checked against Play's hard 30-character app-name and 80-character short-description limits. The audit found and corrected overlong or brand-inconsistent entries in Spanish (Spain/Latin America), Czech, Hindi, Hungarian, Indonesian, French, Italian, Dutch, Polish, Portuguese (Brazil), Romanian, Russian, Swedish, Thai, Ukrainian, and Vietnamese. Final direct-editor verification is 28/28 valid: every locale uses `Ampere Battery Lab`, every app name is 18 characters, every short description is 29–80 characters, and no editor shows the limit warning. The 31 resulting listing changes were moved back to `Für später gespeichert`; none were submitted or published. Human/native review is still recommended before release.
- Play Store settings audit (2026-09-22): app type is `App`, category is `Tools`, and no tags are currently selected. Searches for the evidence-relevant terms `Batter` and `Akku` returned no available tag, so do not add unrelated tags. Store contact currently has the developer email only; no verified phone number or website is configured. External marketing is enabled; leave it enabled unless a deliberate distribution/privacy decision changes.
