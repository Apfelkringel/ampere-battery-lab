# UI/UX notes

Ampere Battery Lab uses a compact, data-first dashboard for battery readings.
The visual system is intentionally calm and high-contrast: a dark/AMOLED mode
for OLED screens, a light mode for bright environments, lime for healthy/live
charging state, blue for neutral telemetry and amber for warnings or wear.

## Responsive behavior

- The overview hero switches to a centered compact composition below 410 dp.
- Charging details switch from three columns to a two-row layout below 380 dp.
- Charts use their real telemetry timestamps, so uneven background sampling does
  not create false equal time gaps.
- Charging and discharging estimates keep mixed, screen-on and screen-off modes
  visibly separate so the measurement basis is not hidden in one combined value.
- Session previews are capped on the overview; the complete list remains in
  History and stays scrollable.
- The activity is not forced into portrait, and Android system-bar insets are
  applied on current Android releases.

## Interaction and accessibility

- Drawn tabs use a 48 dp vertical touch target, a fixed 24 dp icon slot and a
  shared label baseline while preserving the visual tab height.
- Header actions use the same 48 dp bounds, radius and outline geometry; the
  live refresh action is centered as one dot/text group and immediately reads
  Android's current sticky battery broadcast.
- Landscape overview cards reserve separate lanes for the gauge and the live
  status row; redundant status chips are omitted when they would compete for
  the same geometry, and the gauge has an explicit gap below the heading.
- The dashboard exposes the current page, battery state, level and available
  tabs as an accessibility summary, and its Canvas header actions and tabs are
  also exposed as individually focusable virtual Android buttons for TalkBack.
- Settings, backup/restore, export and text entry use native Android controls or
  the system file picker.
- Every interactive area is kept inside the scrollable content and is checked on
  the compact 320 dp test layout as well as the larger emulator layouts.
- Metric cards switch to the stacked icon/label/value composition below 160 dp,
  so the label lane remains readable on 320 dp phones.
- Session history keeps only rows whose stored start/end levels agree with their
  direction; contradictory legacy rows are removed during normalization so the
  list and exports cannot present charging as a battery drop or vice versa.
- Counter jumps beyond three nominal battery capacities are shown as unavailable
  energy rather than as an exaggerated full-cycle value.
- After a long unobserved sampling gap, the current point becomes a fresh
  session baseline instead of being displayed as a continuous measured period.
- Home-screen widgets select separate short, compact and standard RemoteViews
  layouts from their actual width and height; the short layout keeps a single
  readable row for low-height landscape sizes.

The current UI checks cover dark and light themes, all five tabs, compact
charging details, scroll reachability and fatal-exception log scans on Android
API 34. Release verification for API 34, 36 and 37 is recorded in
`docs/VERIFICATION.md`.
