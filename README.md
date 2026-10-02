# DarkFrame

A premium Android customisation app, polished for Samsung Galaxy and foldables.

Choose a look → preview it → apply it.

## The idea

**DarkFrame has no supported-app list.** It themes every application Android legitimately exposes on
the device — including apps installed next month and apps nobody has drawn artwork for.

```
installed app → its own icon → curated DarkFrame glyph if we have one, otherwise the installed icon
              → normalise size, padding and shape → apply the chosen collection → cache → themed icon
```

Handmade DarkFrame artwork is a **quality override**. It never decides coverage: shipping zero
curated icons would not reduce the number of apps DarkFrame themes by one.

Architecture, the hard input cases and the cache model: [`docs/ICON_ENGINE.md`](docs/ICON_ENGINE.md).

## Complete Looks

A look is the unit of choice: an icon collection, a matching wallpaper, matching widgets and a tier,
picked together so the result is coherent. The home screen shows a **real render** of the selected
look — the same wallpaper renderer that will set it, and the same icon renderer that themes the
user's apps — so you can see your phone before you change it.

| | |
| --- | --- |
| **Noir** — free | Deep black, warm white glyphs, hairline keyline |
| **Color Pop** — free | Your apps' own colours on a clean neutral ground |
| **Pure AMOLED** — free | True black, no keyline, maximum contrast |
| **Frost** — Pro | The light one: off-white surfaces, graphite ink |
| **Titanium** — Pro | Brushed graphite and silver, no colour cast |
| **Glass** — Pro | One highlight, one edge |

Palette invariants are enforced by unit test, so a future edit cannot quietly ship a collection whose
glyphs have lost contrast. Details: [`design/COLLECTIONS.md`](design/COLLECTIONS.md).

## Applying, honestly

**No Android API lets an app replace another app's icon system-wide.** The launcher decides what it
draws. DarkFrame therefore prepares the icons and hands over to whatever the device actually
supports, and the primary button is named after that mechanism — never "Apply all".

- **Samsung One UI** — hands over to **Theme Park**, the Good Lock module that applies an icon theme
  across the home screen and app drawer. DarkFrame detects whether Theme Park or Good Lock is
  installed and opens the right one. It does **not** use pinned shortcuts here: on a real Galaxy Z
  Fold8 those produced a second icon beside each original instead of replacing it.
- **Nova, Lawnchair, Smart Launcher, Microsoft, Action, Apex…** — standard `appfilter.xml` icon
  pack; the user selects DarkFrame in the launcher's settings.
- **Pixel Launcher and unknown launchers** — a themed pinned shortcut, offered per app only, plus
  PNG export.

Full per-launcher breakdown: [`docs/LAUNCHER_SUPPORT.md`](docs/LAUNCHER_SUPPORT.md).

## Wallpapers

Drawn on the device from a description, not shipped as files. A Fold needs very different inner and
cover images, and flat fields and fine gradients are exactly what compresses badly and bands
visibly — so every wallpaper is pin-sharp at any panel size for a few kilobytes of code. Eight
categories, each with free content, applied to home, lock or both.

## Performance

Browsing used to make a Fold8 warm. The pipeline now:

- caps preview renders at 192px and snaps sizes into buckets, so a grid never produces export-size
  bitmaps and a fold/unfold re-uses what is already cached;
- scales the source raster with the output instead of a fixed 288px buffer;
- renders on a two-thread, background-priority pool rather than every core;
- evicts only the changed package from the cache instead of wiping every icon;
- debounces package broadcasts, and runs **no service, no alarm and no polling**.

## Package visibility

Two `<queries>` intents plus four named Samsung packages, and no sensitive permissions.
**`QUERY_ALL_PACKAGES` is deliberately not requested.** See
[`docs/PACKAGE_VISIBILITY.md`](docs/PACKAGE_VISIBILITY.md) and [`docs/PRIVACY.md`](docs/PRIVACY.md).

## Build

JDK 17.

```
./gradlew testDebugUnitTest assembleDebug lintDebug bundleRelease
```

CI runs all four on every push and pull request. Release signing is read from Gradle properties
outside the project — see [`docs/RELEASE_SIGNING.md`](docs/RELEASE_SIGNING.md). No key is in this
repository.

## Status

What is done and what still needs a physical device: [`docs/STATUS.md`](docs/STATUS.md).

Third-party trademarks remain the property of their owners. All DarkFrame artwork is original.
