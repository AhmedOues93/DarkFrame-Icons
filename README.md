# DarkFrame Icons

A premium Android customisation app for phones, Samsung Galaxy devices, foldables and large screens.

## The core idea

**DarkFrame has no supported-app list.** It themes every application Android legitimately exposes on
the device — including apps installed next month and apps nobody has drawn artwork for.

```
installed app → its own icon → curated DarkFrame glyph if we have one, otherwise the installed icon
              → normalise size, padding and shape → apply the chosen collection → cache → themed icon
```

Handmade DarkFrame artwork is a **quality override**, used where it exists. It never decides coverage.
Shipping zero curated icons would not reduce the number of apps DarkFrame themes by one.

See [`docs/ICON_ENGINE.md`](docs/ICON_ENGINE.md) for the architecture, the hard cases (adaptive icons,
monochrome layers, legacy baked-in tiles, transparent artwork, unusual aspect ratios, multiple launcher
components) and the cache-invalidation model.

## Collections

Six, all on one shared optical grid, all working with both curated and generated icons:

| | |
| --- | --- |
| **Noir** | Luxury monochrome: deep-black container, warm-white glyph, hairline keyline |
| **Color Pop** | Original brand colours at full clarity on a clean neutral container |
| **Frost** | Premium light finish: off-white container, graphite glyphs |
| **Titanium** | Neutral graphite-to-silver metal, one subtle sweep, no colour cast |
| **Glass** | Restrained translucency: one highlight, one edge, nothing more |
| **Pure AMOLED** | True black, no keyline, maximum contrast |

Details and intent in [`design/COLLECTIONS.md`](design/COLLECTIONS.md). Palette invariants are enforced
by unit test, so a future edit cannot quietly ship a collection whose glyphs have lost contrast.

## What Android does and does not allow

Generating a themed icon and applying it to a launcher are two different things. **No Android API lets
an app replace another app's icon system-wide**, and DarkFrame never claims it can.

What it does instead, chosen per launcher:

- **Icon pack** — ships a standard `appfilter.xml` that Nova, Lawnchair, Smart Launcher, Microsoft
  Launcher and others read. The user selects DarkFrame in that launcher's settings; no API lets us
  select it for them.
- **Pinned themed shortcuts** — the platform pin-shortcut API, which works on Pixel Launcher and
  Samsung One UI Home. It adds an entry rather than replacing an icon, and the app says so.
- **Export** — any themed icon as a 512px PNG, for launchers with a per-app picker or theme engines
  DarkFrame cannot drive.

Samsung One UI Home themes icons only through Galaxy Themes, which Samsung controls, so DarkFrame
offers shortcuts and export there rather than a switch with nothing behind it.

Full per-launcher breakdown: [`docs/LAUNCHER_SUPPORT.md`](docs/LAUNCHER_SUPPORT.md).

## Package visibility

DarkFrame declares two `<queries>` intents — `CATEGORY_LAUNCHER` and `CATEGORY_HOME` — and no
sensitive permissions. **`QUERY_ALL_PACKAGES` is deliberately not requested**: it is broader than the
feature needs, and Google Play restricts it to use cases icon customisation is not among.

The resulting limitations (apps with no launcher activity, work-profile and Secure Folder apps,
policy-hidden packages) are documented in [`docs/PACKAGE_VISIBILITY.md`](docs/PACKAGE_VISIBILITY.md)
and stated to the user in the app.

## Architecture

```
engine/
  domain/     pure Kotlin, no framework imports, unit-tested
              AppIdentity · AppCatalogBuilder · IconStyle(Catalog) · IconNormalizer
              SourceClassifier · ColorMatrices · IconCacheKey · CuratedIconIndex · Monogram
  data/       InstalledAppRepository · CuratedIconRepository · IconCache · AppCatalogWatcher
  render/     ContentBoundsScanner · IconSourceLoader · IconRenderer
  apply/      LauncherCapabilityTable · IconApplyService
  IconResolver · DarkFrameEngine
ui/
  browser/    IconBrowserActivity · IconBrowserViewModel · IconGridAdapter · ThemedIconLoader
  setup/      GuidedSetupActivity
```

`IconRenderer` is the only code in the app that draws an icon. No Activity, Fragment, View or Adapter
contains rendering logic; the view layer receives finished bitmaps through the single-method
`ThemedIconLoader`.

## Large screens and foldables

The browser derives its column count from its measured width rather than from a layout qualifier, so
one layout serves a phone, a Fold's cover screen, the same Fold unfolded, and a tablet — including
across a live fold, which a qualifier-selected span count gets wrong until the activity is recreated.
`sw600dp` raises icon size and margins rather than packing in more, smaller columns.

## Build

JDK 17. Open in Android Studio, or:

```
gradle testDebugUnitTest assembleDebug lintDebug
```

CI runs those three steps on every push to `main` and every pull request, and can be dispatched
manually against a feature branch.

## Status

Current state, and what still needs a physical device, is tracked in
[`docs/STATUS.md`](docs/STATUS.md).

Third-party trademarks remain the property of their owners. All DarkFrame artwork is original.
