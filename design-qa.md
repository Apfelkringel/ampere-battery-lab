# Button design QA

- Source visual truth: `design/batteryhub_dashboard.svg` and the 15 individual
  SVG masters in `design/batteryhub-buttons/`.
- Implementation evidence: `docs/qa/overview-buttons-320x640.png`,
  `docs/qa/health-action-320x640.png`,
  `docs/qa/history-active-spacing-320x640.png`, and
  `docs/qa/history-export-320x640.png`; all five corrected active states are in
  `docs/qa/nav-icon-label-separation-320x640.png`.
- Combined comparison: `docs/qa/button-comparison.png`.
- Source dimensions: 1648 × 928 SVG viewBox (reference PNG: 1672 × 941).
- Implementation viewport: Android 320 × 640 physical pixels at 160 dpi,
  equivalent to 320 × 640 dp at 1× density.
- State: dark theme; Start, Gesundheit and Verlauf active states; benchmark,
  CSV export and chart-range actions visible.

## Full-view comparison evidence

The source is an asset board rather than an application screen, so page-level
layout is intentionally not treated as a 1:1 target. The app preserves the
source's deep-petrol surfaces, cyan outlines, luminous action hierarchy and
complete icon/label compositions while fitting the controls into a five-column
mobile rail and existing content hierarchy.

## Focused comparison evidence

Focused comparison is required because the final navigation buttons are only
about 52 × 42 dp. Each active tab contains one baked SVG-derived icon and label;
no runtime label is visible outside the image. The benchmark and CSV captures
show their icon and exact German copy inside the same rounded surface. The final
CSV capture has fully transparent corners and no black or white export residue.

## Required fidelity surfaces

- Fonts and typography: exact SVG copy is preserved. Mobile tab derivatives
  increase only label size and weight for legibility at 320 dp.
- Spacing and layout rhythm: active-tab icons are raised 16 source units while
  labels remain on a separate 175-unit baseline; action content is centered.
- Colors and visual tokens: source gradients, cyan strokes and foreground colors
  are rasterized directly from the supplied SVGs.
- Image quality and asset fidelity: PNGs are deterministic CoreGraphics exports
  with native transparency; no screenshot crops, placeholders or runtime icon
  approximations are used for active/image-button states.
- Copy and content: Start, Laden, Entladen, Akku, Verlauf, LIVE, Aktiv,
  CSV exportieren, CSV EXPORTIEREN, Starten, 30D and 7D match the masters.

## Comparison history

1. P1: generated tab labels fell onto the lower edge and icon sizes varied.
   Fix: replaced the generated approximations with the supplied individual SVGs.
2. P1: desktop SVG labels became too small in the 320 dp five-column rail.
   Fix: created deterministic mobile SVG derivatives with larger embedded labels.
3. P2: active-tab content had visibly more padding above than below.
   Fix: moved the baked icon/label group upward by 16 source units in all five
   mobile derivatives.
4. P1: Quick Look PNG export left opaque corner residue on the CSV action.
   Fix: switched all runtime exports to direct transparent `sips` conversion.
5. P1: the shared active-tab transform moved labels into the icon slot and made
   some glyphs touch or cover the upper half of their copy after downsampling.
   Fix: each SVG now transforms only its icon primitives; every label stays on
   an independent baseline, guarded by `BatteryButtonAssetLayoutTest`.

No actionable P0, P1 or P2 findings remain in the captured portrait states.

final result: passed
