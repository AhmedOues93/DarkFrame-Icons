# DarkFrame status

## GREEN — done

### Dynamic icon engine
- Themes every app the device exposes, including apps installed in future
- Curated artwork implemented as a quality override, not a supported-app list
- Component-addressed catalog with multi-launcher-activity support
- Adaptive icons cropped to their visible viewport
- Adaptive monochrome layers (API 33+) used as the source for tinted collections
- Legacy opaque icons detected and seated as a clipped inset card
- Transparent / oddly padded artwork normalised by measured content bounds
- Optical-size normalisation with an upscale cap and a wordmark rule
- Monogram fallback for unloadable icons
- Structural cache invalidation (render version + package change stamp in the key)
- Package-sharded disk cache with atomic writes and budget trimming
- Install / update / remove / enable-state tracking while on screen
- Clean architecture: no rendering logic in any Activity, Fragment, View or Adapter

### Collections
- All six implemented as data on one shared optical grid: Noir, Color Pop, Frost,
  Titanium, Glass, Pure AMOLED
- Each works with both curated and dynamically generated icons
- Palette invariants enforced by unit test (contrast, true black, neutral metal,
  translucency bounds)

### Apply, honestly
- Samsung One UI: Theme Park hand-off, with the right step detected per device.
  Pinned shortcuts are no longer the Samsung path — on a real Fold8 they produced
  duplicate icons rather than replacing anything
- Icon-pack `appfilter.xml` for launchers that read it
- Pinned themed shortcuts only where nothing better exists, and only per app
- 512px PNG export through a scoped `FileProvider`
- Per-launcher capability table, narrowed by a runtime probe and never widened
- The Apply screen is driven by that capability, so it cannot describe an unavailable
  mechanism, and its button is disabled rather than firing an intent nothing can serve
- No claim anywhere that DarkFrame can replace icons system-wide

### Pro
- Google Play Billing v7 against `darkframe_pro_lifetime`, a one-time purchase:
  real `ProductDetails` for the price, acknowledgement, restore, and a persistent
  entitlement read by every gated surface
- Pending purchases handled as their own state: a cash or carrier payment neither
  grants nor revokes, and the screen shows it rather than offering checkout again
- A failed query never revokes, so Pro survives being offline; an answered query
  with no purchase does revoke, so refunds are honoured
- Entitlement rules live in a pure `EntitlementPolicy` covered by eleven tests,
  including an exhaustive sweep proving no input grants Pro without a completed
  owned purchase
- Entitlement is granted only for a `PURCHASED` Play purchase — there is no debug
  or fake Pro path anywhere in the code
- Pro collections can be browsed freely and previewed; what the gate withholds is
  applying a look and exporting or pinning its icons

### Package visibility
- Narrow `<queries>` declaration: `CATEGORY_LAUNCHER` and `CATEGORY_HOME` only
- No `QUERY_ALL_PACKAGES`; limitations documented for users and for Play review

### Large screens
- Browser column count derived from measured width, so one layout serves phones,
  Fold cover and inner screens, and tablets, including across a live fold
- `sw600dp` density overrides rather than stretched phone UI

### Project
- Original Classic Outline design system and the first 12 handmade icons, kept
- Curated artwork re-authored as glyphs on transparency so one drawing serves all
  six collections; the original `df_*` tiles stay as icon-pack artwork
- Icon request entry point
- CI: unit tests, debug assemble, lint and release bundle, with reports uploaded on failure
- 162 unit tests covering the engine's domain layer, the optical corrections, the
  catalogs, the capability table, the render-scheduling policy and the billing rules

### Performance
- Preview renders capped and bucketed, so browsing never produces export-resolution
  bitmaps and a fold re-uses the cached ones
- Source raster scales with the output instead of a fixed 288px
- Rendering on a two-thread background-priority pool, not every core
- Per-package cache eviction instead of wiping every icon when one app updates
- Package broadcasts debounced; no services, no polling, no background work

### Fixed — dark tints compressed a colour source

Found by rendering the collections for the first time (SVG reconstruction, not device output).

`ColorMatrices.tintToLuma` maps a source's luma onto the collection tint by multiplication, so the
result spans `0 … tint`. That is right for Noir, Titanium and Pure AMOLED, whose tints are near
white: the full black-to-white range of the source stays visible. For **Frost**, whose tint is
graphite `#23262B`, the same multiplication squeezes everything from black to white into a narrow
dark band, and a colourful source loses its internal structure — a mid-green field and a near-white
mark end up almost the same value.

It does not affect curated glyphs, which are authored as a single value, so it only shows on
generated icons. Fixed: `ColorMatrices.lumaRamp` now ramps from the collection's own container to its tint, so a
black source lands on the surface and a bright one on the ink. Tests pin the separation, pin that the
ink end did not move, and pin that the dark collections are unaffected.

## RED — remaining

### Needs a physical device
- Visual QA of all six collections across real installed apps. The renderer is
  structurally tested and the optical corrections are unit-tested, but no output
  has been looked at on a screen. The debug-only Visual QA matrix (Settings →
  Visual QA, debug builds only) exists for exactly this: every installed app
  across all six collections, with a cell-size control and a magenta ground for
  spotting clipping and transparent gaps
- Samsung Galaxy and Fold QA: cover screen, inner screen, fold/unfold transition
- Verify pinned-shortcut appearance on One UI Home and Pixel Launcher, including
  how each badges or shadows a pinned icon
- Verify DarkFrame appears in Nova / Lawnchair / Smart Launcher icon-pack lists
- Confirm `appfilter.xml` component mappings against installed builds

### Product
- Expand curated artwork well beyond the current 12 glyphs
- Instrumented coverage for the three optical corrections: they are unit-tested as
  maths, and what they do to a real `Canvas` has not been asserted
- Instrumented tests for the render layer (`IconRenderer`, `IconSourceLoader`,
  `ContentBoundsScanner` need a real `Canvas`)
- Icon request form to replace the mailto intent

### Needs Google Play Console
- Billing end to end: `darkframe_pro_lifetime` has to exist as a one-time product
  and be bought through Internal Testing. The client is finished — real
  `ProductDetails`, acknowledgement, restore, persistent entitlement, and no debug
  purchase path at all — but a sideloaded debug APK cannot exercise it
- Store listing: screenshots, feature graphic, Data Safety form (answers are in
  `docs/PRIVACY.md`), hosted privacy policy URL

### Release
- Upload key generated outside the repository and the AAB signed with it; the build
  reads it from properties and `docs/RELEASE_SIGNING.md` has the steps
- Galaxy Themes submission is a separate Samsung channel; decide whether to pursue
