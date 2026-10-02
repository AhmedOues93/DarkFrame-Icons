# Applying icons: what Android actually allows

This document exists so that nobody — user, reviewer or future contributor — has to guess what
DarkFrame can do. The product's rule is that it never claims a capability Android does not grant.

## The core limitation

**Generating a themed icon and applying it to a launcher are two different things.**

There is no Android API — public, hidden-but-callable, permission-gated or vendor-specific — that
lets an installed app replace another app's icon across the system. The launcher owns what the
launcher draws. There is no `setIconForPackage()`, no AppOp, no manifest flag and no Play-allowed
vendor hook.

Any product that claims to silently restyle your home screen on stock Android is doing one of three
things: it is itself the launcher, it requires root or Xposed, or it is lying.

DarkFrame therefore ships three real mechanisms and picks between them per launcher.

## Mechanism 1 — Icon pack (`appfilter.xml`)

Several third-party launchers implement a long-standing community convention: an app declares one of
a set of icon-pack intent actions, ships `res/xml/appfilter.xml` mapping components to drawables, and
the launcher substitutes icons itself.

DarkFrame declares those actions on `MainActivity` and ships `appfilter.xml`.

| Property | Reality |
| --- | --- |
| Who applies the icons | The launcher, not DarkFrame |
| Can DarkFrame enable it for the user? | **No.** The user must select DarkFrame in that launcher's own settings. No launcher exposes an API to do it for them. |
| Coverage | Only apps listed in `appfilter.xml`, because a launcher can only draw drawables that exist in our APK |
| Works with dynamically generated icons? | **No.** Generated icons are per-device bitmaps; the format can only reference packaged drawables |
| Works on Pixel Launcher / One UI Home? | **No** |

This is why `appfilter.xml` and `df_curated.xml` are separate files. They are not duplicates of each
other — they are two different contracts:

| | `res/xml/appfilter.xml` | `res/xml/df_curated.xml` |
| --- | --- | --- |
| Read by | Third-party launchers | DarkFrame's own engine |
| Format | Fixed by the icon-pack convention | Ours |
| Points at | Complete pre-composed tiles (`df_*`) | Glyphs on transparency (`dfg_*`) |
| Composition | None; drawn as-is | Engine composes container + treatment at runtime |
| Coverage | The listed apps | Every installed app; entries only upgrade quality |

## Mechanism 2 — Pinned shortcuts

`ShortcutManagerCompat.requestPinShortcut()` asks the launcher to place a home-screen shortcut
carrying a bitmap we supply. This is a fully supported platform API and it works on Pixel Launcher
and Samsung One UI Home.

Its limits are stated in the UI, not buried here:

- it **adds** an entry; it does not replace the app's own icon;
- the app drawer is unchanged;
- some launchers add their own shadow or badge to pinned shortcuts;
- the launcher normally shows a confirmation dialog, and there is no reliable callback telling us
  whether the icon ended up on the home screen. DarkFrame reports "request sent", never "applied".

## Mechanism 3 — Export

Any themed icon can be written out as a 512px PNG and shared. This is the universal fallback and
the route for launchers with a per-app icon picker (for example Niagara), and for theme engines
DarkFrame cannot drive.

## Per-launcher support

Encoded as data in `LauncherCapabilityTable` and unit-tested, so it cannot drift away from what the
UI says.

| Launcher | Mechanism | Notes |
| --- | --- | --- |
| Nova, Lawnchair, Microsoft Launcher, Smart Launcher, Action, Apex, Solo, GO | Icon pack | User selects DarkFrame in the launcher's settings |
| Niagara | Per-app picker | One icon at a time; does not read icon packs |
| Pixel Launcher | Pinned shortcut + export | No third-party icon-pack support |
| Samsung One UI Home | Pinned shortcut + export | See below |
| Launcher3 / AOSP | Pinned shortcut + export | No icon-pack support |
| Anything unrecognised | Export, upgraded to pinned shortcut only if the platform confirms support | Conservative by default |

### Samsung One UI Home

One UI Home does not read third-party icon packs. It themes icons only through **Galaxy Themes**, a
distribution channel Samsung controls; an installed app cannot set or install an icon theme into it.

So on a Galaxy device DarkFrame offers pinned shortcuts and export, with a guided explanation — and
does **not** offer a "apply to One UI" switch, because there is nothing behind it. Publishing a
Galaxy Themes icon pack is a separate Samsung submission with its own tooling, and is tracked as a
distribution question rather than an in-app feature.

### Pixel Launcher and themed icons

Pixel Launcher's "Themed icons" feature recolours icons from their adaptive **monochrome** layer
using Material You. It is Google's feature, applied by the launcher, and it cannot be pointed at a
third-party pack.

DarkFrame uses the same monochrome layer as a *source* for its tinted collections (Noir, Frost,
Titanium, Pure AMOLED), which is why those collections look clean on apps that ship one — but that is
DarkFrame rendering its own icon, not DarkFrame driving Pixel Launcher.

## What this means for the roadmap

The highest-leverage way to apply generated icons to a whole home screen is for the launcher to ask
us for them. That is a launcher integration, not an API we are missing, and it is tracked as such.
Until then, DarkFrame is honest about offering: icon-pack support where it exists, themed shortcuts
where it does not, and export everywhere.
