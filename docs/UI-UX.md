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
- Session previews are capped on the overview; the complete list remains in
  History and stays scrollable.
- The activity is not forced into portrait, and Android system-bar insets are
  applied on current Android releases.

## Interaction and accessibility

- Drawn tabs have a 48 dp vertical touch target while preserving the visual tab
  height.
- The dashboard exposes the current page, battery state, level and available
  tabs as an accessibility summary for screen readers.
- Settings, backup/restore, export and text entry use native Android controls or
  the system file picker.
- Every interactive area is kept inside the scrollable content and is checked on
  the compact 320 dp test layout as well as the larger emulator layouts.

The current UI checks cover dark and light themes, all five tabs, compact
charging details, scroll reachability and fatal-exception log scans on Android
API 34. Release verification for API 34, 36 and 37 is recorded in
`docs/VERIFICATION.md`.
