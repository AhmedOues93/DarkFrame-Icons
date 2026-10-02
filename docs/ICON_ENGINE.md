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
| `OpticalMetrics` | domain | Area, centring and separation corrections |
| `SourceClassifier` | domain | Glyph vs. legacy full-bleed artwork, and how to seat each |
| `ColorMatrices` | domain | Perceptual saturation/tint matrices; WCAG contrast |
| `IconCacheKey`, `StableHash` | domain | Cache identity and stable on-disk naming |
| `CuratedIconIndex` | domain | Override lookup: component first, then package |
| `InstalledAppRepository` | data | Enumerates launcher activities via `<queries>` |
| `CuratedIconRepository` | data | Parses `df_curated.xml`, resolves drawable ids |
| `IconCache` | data | Memory LRU over a package-sharded disk store |
| `AppCatalogWatcher` | data | Install / update / remove / enable-state changes |
| `ContentBoundsScanner` | render | One strided pass → bounds, density, mass centre, luma, saturation |
| `IconSourceLoader` | render | Rasterises whatever the app ships into usable artwork |
| `IconRenderer` | render | **The only code in the app that draws an icon** |
| `IconResolver` | engine | The flow above, with caching and a per-thread renderer |
| `IconApplyService` | apply | The three real apply mechanisms |
| `IconSetPreparer` | apply | Renders a whole collection out as PNGs, for the Samsung hand-off |
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
a small tile floating inside a bigger tile. `SourceClassifier` recognises the case (high opacity *and*
near-full canvas coverage) and the renderer commits to it — the artwork is seated at 94% of the tile
and clipped to DarkFrame's own corner radius, so the collection *frames* it rather than nesting it.

The earlier value was 0.78, which is what a double background looks like: the app's own tile clearly
visible inside DarkFrame's. The thin frame also means every tile in a grid is the same size, whether
the app shipped a glyph or a square.

### Transparent and oddly padded icons
Handled by the premise behind the whole normaliser: size is derived from measured **content bounds**,
never from the source canvas. A glyph occupying 35% of its canvas and one occupying 95% end up the
same optical size.

### Circular logos, and sparse ones
Normalising bounding boxes is only half the job, and it is the half that looks right in a screenshot
of six hand-picked apps and wrong in a grid of two hundred real ones. A circle inscribed in its box
covers 78.5% of it, so a circular logo normalised to the same box as a square one reads visibly
smaller — Android's own keyline grid encodes the same compensation at 176/192 against 152/192.

`OpticalMetrics.areaCompensation` equalises ink *area* instead, scaling by `1/sqrt(fill)`. That is
exact at the circle end and too aggressive at the sparse end, so it is clamped rather than damped: a
clamp keeps the circle case exact where a damping exponent would compromise it to "improve" a case
that should not be corrected at all. A hairline wordmark is therefore not blown up to match the ink
mass of a solid square.

### Asymmetric glyphs
A play triangle, a comma, a location pin: the bounding box is centred but the ink is not. The glyph is
shifted a third of the way towards its alpha-weighted centre of mass, capped at 8% of its own extent.
Partial on purpose — full centroid alignment over-corrects a shape with one heavy limb, and a cap
stops one faint far-corner pixel dragging the glyph off the tile.

### Very bright and very dark icons on a colour-preserving collection
Color Pop and Glass keep the app's own colours, which means an app whose artwork happens to sit at the
container's own luminance disappears into it. Real on Glass, where dark-mode-first app icons are a
whole population. `OpticalMetrics.separationLift` pushes a source within 0.14 gamma luma of its ground
clear of it, away from whichever side the container is on, bounded so a lift cannot blow out to flat
white or crush to flat black. It returns zero for the overwhelming majority of icons.

### Multicolour icons that would read as neon
The pull-back is per icon, not global. A global desaturation takes the most character out of exactly
the muted, carefully chosen brand palettes that needed no help, so `OpticalMetrics.vibrancy` leaves
anything under 0.28 mean saturation completely alone and applies the style's full value only above
0.74.

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
| App updated | New `lastUpdateTime` → new key → cache miss → re-render | Only to re-read the catalog (see below) |
| Renderer or palette changed | Bump `IconStyle.RENDER_VERSION` | No |
| Different size or collection requested | Part of the key | No |
| App uninstalled | `invalidatePackage()` deletes the package's shard directory | Yes, housekeeping only |
| User asks to rebuild | `invalidateAll()` | — |
| Disk budget exceeded | `trimToBudget()` evicts oldest files | — |

The one thing a signal *is* needed for is re-reading the catalog. The key is derived from the
`AppIdentity` held in memory, so an identity still carrying the pre-update change stamp would keep
producing the pre-update key — and therefore the pre-update icon. `AppCatalogWatcher` reloads the
catalog on any package change; the cache miss then follows from the fresh stamp. Nothing else about a
stale cache entry can be served by accident.

`StableHash` is FNV-1a rather than `String.hashCode()` on purpose: the hashes name files on disk and
must still match after an app upgrade, which `hashCode()` does not contractually guarantee, and 32
bits is not enough headroom for a few thousand apps across six collections and several sizes.

## Collections

All six share one optical grid — the same corner geometry (22.5% of the edge), the same ratio-based
keyline weight, and glyph sizes confined to a narrow band — so switching collections never changes
how large icons *feel*. What differs is surface and glyph treatment only.

| Collection | Container | Glyph | Tier |
| --- | --- | --- | --- |
| **Noir** | `#0B0C0F`, flat, hairline keyline | Luma ramp → warm white `#F2F1EE`, gain 1.12 | Free |
| **Color Pop** | `#17191D`, flat, clean neutral | Original brand colours, up to 0.94 saturation | Free |
| **Frost** | `#F2F1ED` off-white, top bloom, bright rim | Luma ramp → graphite `#23262B`, linear | Pro |
| **Titanium** | `#2A2D32` → `#474B52`, 135° sweep, lit and shaded bevel | Luma ramp → `#E4E7EB`, gain 1.06 | Pro |
| **Glass** | `#CC14171B` translucent, pane highlight and floor | Original colours, up to 0.90 saturation | Pro |
| **Pure AMOLED** | True `#000000`, no keyline | Luma ramp → pure white, gain 1.22 | Free |

Each collection owns exactly one surface finish, and a unit test enforces that no two share one: a
shared finish means two collections with the same material identity, which is the failure the six
exist to avoid. The finishes are drawn **over** the glyph, so content on Glass reads as being behind
the pane and metal's bevel is the surface's own edge.

The two monochrome collections carry a contrast gain on the luma ramp, which is what separates a
designed monochrome conversion from a desaturate filter. The gain works by overshooting the ramp's
ends and letting `ColorMatrix`'s channel clamp turn the overshoot into a real toe and shoulder — so it
only leaves a palette intact where its ink and surface sit at or near the channel extremes. Noir and
Pure AMOLED qualify. Frost does not, is explicitly linear, and a test pins how far any gain may move a
collection's own ink.

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
