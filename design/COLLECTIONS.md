# DarkFrame visual collections

DarkFrame is not dark-only. Every collection renders the same app identity on the same optical grid,
and differs only in surface and glyph treatment.

These are the values the engine actually ships — see `IconStyleCatalog`, which is the single source of
truth. This document explains the intent behind them.

## Shared grid

Identity constants, the same in all six collections:

| Property | Value |
| --- | --- |
| Corner radius | 22.5% of the edge length |
| Keyline weight | 0.75% of the edge length (hairline at any size) |
| Glyph size band | 50–58% of the edge length |
| Full-bleed artwork | seated at 78%, clipped to a concentric inner radius |
| Upscale cap | 2.6x |

Everything is a ratio, so an icon is identical at 48px in a grid and at 512px in an export.

## Noir — free
Luxury monochrome. Container `#0B0C0F`, flat, with a 10%-white hairline keyline. The glyph's luma is
mapped onto warm white `#F2F1EE` rather than flattened to a silhouette, so interior detail survives.
Prefers an adaptive monochrome layer where one exists.

## Color Pop — free
Pixel/iOS-grade clarity. A clean neutral `#17191D` container that gets out of the way, with the app's
own brand colours at saturation 0.94 — held just under 1.0 so over-saturated source artwork cannot
read as neon, and not far enough under for a brand colour to look wrong next to its own app. The
largest glyph of the dark collections (56%), because recognisability is the whole point.

## Frost — pro
The light collection. Off-white `#F2F1ED` container with a dark hairline, glyph luma mapped onto
graphite `#23262B`. Premium rather than merely inverted: the off-white is warm, not paper-white, and
the graphite is soft, not black.

## Titanium — pro
Neutral brushed metal. `#2A2D32` to `#474B52` in a single two-stop sweep on the 135° diagonal — that
is the entire treatment. Stacked gradients and specular streaks are what make metal effects look
cheap; a narrow neutral ramp reads as graphite. Both stops are tested for colour cast, so it can
never drift blue or warm.

## Glass — pro
Restrained translucency. `#CC14171B` with one top-down highlight that falls off before the midpoint,
plus a 20%-white edge. One reflection, no bottom glow: the moment a second highlight is added it stops
reading as a material and starts reading as a skeuomorphic button.

## Pure AMOLED — free
True `#000000`, no keyline at all, pure-white glyph, and the largest glyph in the set (58%). The tile
is meant to disappear into a black wallpaper, leaving only the glyph — which a keyline would defeat.

## Rules

- All artwork is original. Brand glyphs may reference recognisable trademarks only as far as needed to
  identify the corresponding app; no third-party icon-pack artwork is reused, and DarkFrame's visual
  identity (its grid, corner geometry, keyline treatment and glyph language) is its own.
- Curated glyphs are authored on transparency with no background of their own, so one drawing works in
  all six collections.
- Every variant must stay legible at 48px.
- Palette invariants are unit-tested, not eyeballed: tinted collections must clear 7:1 WCAG contrast,
  Pure AMOLED must be true black and keyline-free, Titanium's sweep must stay subtle and neutral,
  Glass must be translucent without going sheer.
