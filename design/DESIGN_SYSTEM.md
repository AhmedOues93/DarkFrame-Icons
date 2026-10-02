# DarkFrame Design System

## The optical grid

Every DarkFrame icon, curated or generated, sits on one grid. These are ratios rather than pixel
values, so an icon is identical at 48px in a launcher and at 512px in an export.

| Property | Value |
| --- | --- |
| Design canvas | 512 × 512 (authoring canvas for glyphs is 108) |
| Corner radius | 22.5% of the edge length |
| Keyline weight | 0.75% of the edge length |
| Glyph long edge | 50–58% of the edge length, by collection |
| Full-bleed artwork | 78%, clipped to a concentric inner radius |
| Upscale cap | 2.6× |
| Wordmark threshold | aspect ratio above 2.4:1 is fitted tighter |

Optical centering beats mathematical centering; each collection carries an optical lift ratio for
when that matters.

The single most important consequence: icon size is derived from **measured opaque content**, never
from the source canvas. That is what makes a grid of mixed adaptive, legacy and transparent artwork
look designed rather than assembled.

## Glyph language

- Consistent stroke weight across a collection; no weight changes to fit a shape.
- Simplified forms that stay legible at 48px.
- No gradients in glyphs. Surface finishes belong to the container, not the mark.
- Curated glyphs are authored on transparency in white, with no background, so one drawing works in
  every collection.

## Collections

Six, all on the grid above, differing only in container surface and glyph treatment: Noir, Color Pop,
Frost, Titanium, Glass and Pure AMOLED. Values, intent and the invariants enforced by unit test are in
[`COLLECTIONS.md`](COLLECTIONS.md); `IconStyleCatalog` is the source of truth.

The earlier "Classic Outline / AMOLED / Glass / Chrome" grouping has been superseded: Classic Outline
became Noir, Chrome became Titanium, and Color Pop and Frost were added so the system is not
dark-only.

## Product surface

The app's own chrome is a separate palette from the icon collections, in `res/values/colors.xml`.
Backgrounds run near-black (`#07080A`) with raised surfaces at `#101216` and `#171A1F`, text in a
three-step neutral ramp. Large screens get larger icons and wider margins, not more columns of the
same size.

## Originality

All artwork is original. Brand glyphs may reference recognisable trademarks only as far as needed to
identify the corresponding app. No third-party icon-pack artwork is reused, and DarkFrame's identity —
its grid, corner geometry, keyline treatment and glyph language — is its own.
