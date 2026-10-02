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
- Icon-pack `appfilter.xml` for launchers that read it
- Pinned themed shortcuts via the platform API
- 512px PNG export through a scoped `FileProvider`
- Per-launcher capability table, narrowed by a runtime probe and never widened
- Guided setup driven by that capability, so it cannot describe an unavailable mechanism
- No claim anywhere that DarkFrame can replace icons system-wide

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
- CI: unit tests, debug assemble, lint, with reports uploaded on failure
- 76 unit tests covering the engine's domain layer

## RED — remaining

### Needs a physical device
- Visual QA of all six collections across real installed apps (the renderer is
  structurally tested; its *output* has not been looked at on a screen yet)
- Samsung Galaxy and Fold QA: cover screen, inner screen, fold/unfold transition
- Verify pinned-shortcut appearance on One UI Home and Pixel Launcher, including
  how each badges or shadows a pinned icon
- Verify DarkFrame appears in Nova / Lawnchair / Smart Launcher icon-pack lists
- Confirm `appfilter.xml` component mappings against installed builds

### Product
- Expand curated artwork well beyond the current 12 glyphs
- Google Play Billing: `EntitlementProvider` still returns FREE by design; no
  Pro content is gated by a fake purchase
- Instrumented tests for the render layer (`IconRenderer`, `IconSourceLoader`,
  `ContentBoundsScanner` need a real `Canvas`)
- Icon request form to replace the mailto intent

### Release
- Store screenshots, feature graphic, privacy policy
- Release signing and AAB
- Galaxy Themes submission is a separate Samsung channel; decide whether to pursue
