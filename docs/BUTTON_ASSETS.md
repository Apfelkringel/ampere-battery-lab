# BatteryHub button assets

The button artwork used by Ampere 0.286 comes from the user-supplied individual
SVG masters in `design/batteryhub-buttons/`. Copy, icon geometry, type placement,
gradients and radii remain part of each source asset; the app does not redraw or
reposition those elements at runtime. Locale-specific variants for buttons with
baked-in labels live in `design/batteryhub-buttons-{en,es,fr,it,pt-BR,nl}/`;
their shape and geometry match the German masters, and only the label changes.

## Deterministic export

- Source format: SVG, one self-contained file per visible button state.
- Runtime format: transparent PNG in `android/app/src/main/res/drawable-nodpi/`,
  with matching resources in the locale-qualified `drawable-*-nodpi/` folders.
- Export: native SVG pixel dimensions via macOS CoreGraphics (`sips`), retaining
  the source transparency directly without screenshot crops or flood filling.
- Post-processing: none; copy, icon, gradient, radius and alpha all come from
  the corresponding SVG master.
- Runtime behavior: the complete bitmap moves inward by 1 dp while pressed;
  icon and label therefore remain locked together.

## Mapping

- `01_laden.svg` through `05_start_card.svg`: active portrait navigation.
- `06_live.svg` and `07_menu.svg`: header controls.
- `08_brightness.svg`: source-only master; the removed theme control has no runtime PNG.
- `09_aktiv.svg`: active benchmark state.
- `10_csv_small.svg` and `11_csv_large.svg`: CSV actions.
- `12_starten.svg` and `15_start_wide.svg`: benchmark start actions.
- `13_30d.svg` and `14_7d.svg`: chart range actions.

## Locale variants

Android selects label-localized PNGs for English, Spanish, French, Italian,
Brazilian Portuguese, and Dutch:

- `01_laden.svg` through `05_start_card.svg`: navigation cards.
- `09_active.svg`: active benchmark state.
- `10_csv_small.svg` and `11_csv_large.svg`: CSV export actions.
- `12_start.svg`: benchmark start action.

The remaining embedded labels are language-neutral (LIVE and day ranges). Keep
every locale artwork variant at the same dimensions as its German source so all
supported languages preserve the existing button layout.

The original dashboard composition is retained as
`design/batteryhub_dashboard.svg` for visual provenance.

The five files in `design/batteryhub-buttons-mobile/` are deterministic mobile
derivatives of the first five masters. The embedded label size/weight is
increased so the text remains readable when a full card is reduced to a compact
five-column 320 dp navigation rail. Icons and labels use separate vertical
slots inside each image, preventing their pixels from colliding; no runtime
text overlay is introduced.

The SHA-256 manifest at `design/button-assets.sha256` pins runtime button PNGs
for every supported locale.
