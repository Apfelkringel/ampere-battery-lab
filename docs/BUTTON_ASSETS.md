# BatteryHub button assets

The button artwork used by Ampere 0.285 comes from the user-supplied individual
SVG masters in `design/batteryhub-buttons/`. Copy, icon geometry, type placement,
gradients and radii remain part of each source asset; the app does not redraw or
reposition those elements at runtime.

## Deterministic export

- Source format: SVG, one self-contained file per visible button state.
- Runtime format: transparent PNG in `android/app/src/main/res/drawable-nodpi/`.
- Export: native SVG pixel dimensions via macOS CoreGraphics (`sips`), retaining
  the source transparency directly without screenshot crops or flood filling.
- Post-processing: none; copy, icon, gradient, radius and alpha all come from
  the corresponding SVG master.
- Runtime behavior: the complete bitmap moves inward by 1 dp while pressed;
  icon and label therefore remain locked together.

## Mapping

- `01_laden.svg` through `05_start_card.svg`: active portrait navigation.
- `06_live.svg` through `08_brightness.svg`: header controls.
- `09_aktiv.svg`: active benchmark state.
- `10_csv_small.svg` and `11_csv_large.svg`: CSV actions.
- `12_starten.svg` and `15_start_wide.svg`: benchmark start actions.
- `13_30d.svg` and `14_7d.svg`: chart range actions.

The original dashboard composition is retained as
`design/batteryhub_dashboard.svg` for visual provenance.

The five files in `design/batteryhub-buttons-mobile/` are deterministic mobile
derivatives of the first five masters. Only the embedded label size/weight is
increased so the text remains readable when a full card is reduced to a compact
five-column 320 dp navigation rail; no runtime text overlay is introduced.

The SHA-256 manifest at `design/button-assets.sha256` pins every runtime PNG
used by the release.
