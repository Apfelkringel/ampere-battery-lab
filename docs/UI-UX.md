# UI/UX notes

## Visual language

Ampere pairs precise battery telemetry with a warm editorial character. The
overview is intentionally led by a human battery-care illustration rather than
another technical chart: it makes the product feel helpful before the detailed
measurements begin. Deep petrol is the anchor, luminous turquoise communicates
live energy, pale mint separates supporting information and warm cream keeps
long-form values comfortable to read. Non-critical accents stay inside this
turquoise family so the interface feels like one authored world.

Large asymmetric curves, 24 dp card corners, small energy marks and a display
serif for emotional headings provide the charm of the visual references. The
compact sans-serif remains reserved for telemetry, units and controls. This
keeps the app expressive without weakening the accuracy or scanability of its
battery data.

The hierarchy uses three deliberately different surface levels: illustrated
hero status, compact primary metrics and quieter secondary analysis or settings
surfaces. Smaller cards use 18 dp radii and softer outlines instead of inheriting
the hero card silhouette. The page name appears only once above navigation, and
build state sits beside the Ampere wordmark as a compact badge. Empty history
states retain honest chart grammar (scale, grid, time axis and an explicitly
absent series) rather than presenting a decorative curve as data.

Interactive controls use one Ampere key language: solid faces, a restrained
two-dp lower edge, consistent corner geometry and a true pressed depth. Filled
turquoise indicates actions or the active destination; a small terminal mark
replaces decorative gloss and keeps buttons distinct from passive cards.
The visible button faces are purpose-made, generated raster artwork rather than
generic programmatic fills. Three transparent masters cover wide actions,
active navigation and square header controls. Native text and vector glyphs are
layered over the artwork from the same measured center, preserving crisp type,
exact icon alignment, localization and accessibility at every screen density.

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
  also exposed as individually focusable virtual Android buttons for TalkBack;
  their screen bounds follow the current scroll position.
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
