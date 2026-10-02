# Roadmap

Milestone 1 described a fixed icon pack. The product has since moved to a dynamic engine, which
changes what the remaining milestones mean: icon *count* is no longer the measure of progress,
because coverage is already every app on the device. What is left to prove is quality on real
artwork, and behaviour on real hardware.

## Milestone 1 — engine ✅
- Dynamic icon engine covering every app the device exposes, future installs included
- Six collections on one shared optical grid, as data
- Curated artwork as a quality override, authored as glyphs that work in all six
- Honest, per-launcher apply model with guided setup
- Narrow package visibility, documented for users and for Play review
- Clean layering with a device-free, unit-tested domain
- CI green on tests, assemble and lint

## Milestone 2 — visual proof on hardware
The engine is structurally tested; its *output* has not yet been looked at on a screen.
- Render every collection across a real app drawer and review the result
- Galaxy Z Fold: cover display, inner display, and the fold transition with the browser open
- Legacy baked-in-tile apps: confirm the inset-card treatment looks deliberate, not accidental
- Adaptive monochrome sources: confirm the tinted collections read as intended
- Pinned shortcuts on One UI Home and Pixel Launcher, including each launcher's own badging
- Confirm DarkFrame appears in Nova / Lawnchair / Smart Launcher icon-pack pickers
- Instrumented tests for the render layer, which needs a real `Canvas`

## Milestone 3 — curated depth
- Expand curated glyphs along the priority order in `ICON_SCALE_PLAN.md`
- Icon request form replacing the mailto intent, framed as requesting better artwork rather than
  requesting support
- Wallpapers and widgets kept optional and secondary to icons

## Milestone 4 — store
- Google Play Billing wired behind the existing `EntitlementProvider`; no Pro content gated by a
  fake purchase until it is
- Screenshots, feature graphic, privacy policy, store listing
- Release signing and signed AAB
- Decide whether to pursue a Galaxy Themes submission, which is a separate Samsung channel with its
  own tooling — see `LAUNCHER_SUPPORT.md`
