# The dynamic icon engine

## The premise

DarkFrame does not have a supported-app list.

It themes **every application the device legitimately exposes**, including apps nobody has drawn
artwork for and apps that do not exist yet. Handmade DarkFrame artwork exists, and is used where it
exists, but it is a *quality override* — it never decides coverage.

That distinction drives the whole design. The code path for "an app we have never seen" is the
**normal** path, not a fallback, and it is exercised by the vast majority of apps on any real device.

## The flow

```
installed app
  └─ read package + launcher component + the app's own icon
      └─ is there a curated DarkFrame glyph for this component or package?
          ├─ yes → use the curated glyph
          └─ no  → use the app's own installed icon          ← the normal case
              └─ normalise: measure content, crop bleed, fix optical size
                  └─ apply the selected collection: container, finish, glyph treatment
                      └─ cache under a key that fully describes the result
                          └─ themed icon
```

One call: `IconResolver.resolve(identity, style, sizePx)`.

## Components

| Type | Layer | Responsibility |
| --- | --- | --- |
| `AppIdentity` | domain | One launchable component. Component-addressed, not package-addressed |
| `AppCatalogBuilder` | domain | Dedupe, ordering, multi-launcher disambiguation |
| `IconStyle`, `IconStyleCatalog` | domain | The six collections, as data |
| `IconNormalizer` | domain | Optical-size maths; adaptive viewport crop |
| `SourceClassifier` | domain | Glyph vs. legacy full-bleed artwork, and how to seat each |
| `ColorMatrices` | domain | Perceptual saturation/tint matrices; WCAG contrast |
| `IconCacheKey`, `StableHash` | domain | Cache identity and stable on-disk naming |
| `CuratedIconIndex` | domain | Override lookup: component first, then package |
| `InstalledAppRepository` | data | Enumerates launcher activities via `<queries>` |
| `CuratedIconRepository` | data | Parses `df_curated.xml`, resolves drawable ids |
| `IconCache` | data | Memory LRU over a package-sharded disk store |
| `AppCatalogWatcher` | data | Install / update / remove / enable-state changes |
| `ContentBoundsScanner` | render | Strided alpha scan → content bounds + opacity |
| `IconSourceLoader` | render | Rasterises whatever the app ships into usable artwork |
| `IconRenderer` | render | **The only code in the app that draws an icon** |
| `IconResolver` | engine | The flow above, with caching and a per-thread renderer |
| `IconApplyService` | apply | The three real apply mechanisms |
| `LauncherCapabilityTable` | apply | Per-launcher capability, as tested data |

Everything in `domain/` and `apply/LauncherCapability.kt` is free of Android framework imports, and
is covered by JVM unit tests. No Activity, Fragment, View or Adapter contains rendering logic; the
view layer is handed finished bitmaps through the single-purpose `ThemedIconLoader` interface.

## The hard cases, and how each is handled

### Adaptive icons
Authored on a 108dp canvas of which only the inner 72dp is guaranteed visible — the rest is bleed
for the launcher's mask and parallax. Rendering the full canvas makes every adaptive icon look about
a third too small next to a legacy one, so the engine crops to the visible viewport before measuring.

### Adaptive monochrome layers (API 33+)
Exactly the asset the tinted collections want. When a style sets `preferMonochromeLayer` (Noir,
Frost, Titanium, Pure AMOLED) and the icon has one, it is used as the source, and the renderer
applies a *flat* tint rather than a luma-mapped one — a mask carries no colour information, so
multiplying its luma into the tint would just produce a dim glyph.

### Legacy icons with a baked-in tile
A pre-adaptive opaque square or circle. Treated as a glyph, it produces the classic amateur result:
a small tile floating inside a bigger tile. `SourceClassifier` recognises the case (high opacity
*and* near-full canvas coverage) and the renderer commits to it — the artwork is seated much larger
and clipped to DarkFrame's own corner radius, so it reads as one deliberate inset card.

### Transparent and oddly padded icons
Handled by the premise behind the whole normaliser: size is derived from measured **content bounds**,
never from the source canvas. A glyph occupying 35% of its canvas and one occupying 95% end up the
same optical size.

### Unusual aspect ratios
Content more elongated than 2.4:1 is treated as a wordmark and fitted slightly tighter so it does not
run edge to edge. The threshold sits well clear of 2:1, which is an ordinary logo proportion.

### Tiny source artwork
Upscaling is capped at 2.6x. Past that the result is visibly soft, and slightly small beats blurry.

### Icons that will not load
A corrupt install, or a package that disappears mid-render. The engine renders a styled monogram
from the app's label rather than leaving a hole in the grid.

### Multiple launcher components
Kept as separate entries, because a launcher shows each of them separately. The alphabetically first
activity keeps the bare label and the rest are suffixed, deterministically, so a row's label does not
change between launches.

### Newly installed, updated and removed apps
`AppCatalogWatcher` listens while the browser is on screen. It distinguishes a genuine uninstall from
the `REMOVED` + `ADDED` pair the platform sends during an update, so a cache is not wiped for an app
that is about to come straight back.

## Cache invalidation

The design goal is that correctness should not depend on a signal arriving. Most invalidation is
therefore structural: the key `IconCacheKey.of()` builds contains the render version, style id, pixel
size, curated-or-not, the component, **and the installed package's `lastUpdateTime`**.

| Event | How it is handled | Needs a signal? |
| --- | --- | --- |
| App updated | New `lastUpdateTime` → new key → cache miss → re-render | No |
| Renderer or palette changed | Bump `IconStyle.RENDER_VERSION` | No |
| Different size or collection requested | Part of the key | No |
| App uninstalled | `invalidatePackage()` deletes the package's shard directory | Yes, housekeeping only |
| User asks to rebuild | `invalidateAll()` | — |
| Disk budget exceeded | `trimToBudget()` evicts oldest files | — |

`StableHash` is FNV-1a rather than `String.hashCode()` on purpose: the hashes name files on disk and
must still match after an app upgrade, which `hashCode()` does not contractually guarantee, and 32
bits is not enough headroom for a few thousand apps across six collections and several sizes.

## Collections

All six share one optical grid — the same corner geometry (22.5% of the edge), the same ratio-based
keyline weight, and glyph sizes confined to a narrow band — so switching collections never changes
how large icons *feel*. What differs is surface and glyph treatment only.

| Collection | Container | Glyph | Tier |
| --- | --- | --- | --- |
| **Noir** | `#0B0C0F`, flat, hairline keyline | Luma → warm white `#F2F1EE` | Free |
| **Color Pop** | `#17191D`, flat, clean neutral | Original brand colours, saturation 0.94 | Free |
| **Frost** | `#F2F1ED` off-white, dark hairline | Luma → graphite `#23262B` | Pro |
| **Titanium** | `#2A2D32` → `#474B52`, one 135° sweep | Luma → `#E4E7EB` | Pro |
| **Glass** | `#CC14171B` translucent, one top highlight | Original colours, saturation 0.90 | Pro |
| **Pure AMOLED** | True `#000000`, no keyline | Luma → pure white | Free |

Design guard rails are enforced by unit test, not by eye: every tinted collection must clear a 7:1
WCAG contrast ratio between glyph and container; Titanium's two stops must stay within a narrow
luminance delta and show no colour cast; Glass must be translucent but not sheer; Pure AMOLED must be
true black with a fully transparent keyline. A future palette edit that breaks one of those fails the
build.

Both curated glyphs and generated icons go through the identical pipeline, which is why they sit
together in a grid without looking like two different products.

## Thread model

Every engine method is blocking and documented `@WorkerThread`; the engine does not hide a dispatcher
inside itself, so a screen owns its own concurrency and can cancel a burst of work it no longer
needs. `IconRenderer` reuses `Paint` and geometry objects and is therefore **not** thread-safe:
`IconResolver` gives each thread its own instance via `ThreadLocal` rather than serialising the grid
behind one lock.

`IconResolver.peek()` is the one main-thread-safe call — an in-memory lookup, used so an already
rendered grid scrolls without a placeholder flicker.
