# DarkFrame product architecture

DarkFrame is an icon-first, offline-first Android customization product. There is no network code in
the icon engine at all.

## Content

Two distinct kinds of content, often confused, deliberately separated here:

1. **Generated icons** — produced on device from the installed app's own icon, for every app the
   device exposes. This is the product's coverage model and it has no list behind it.
2. **Curated artwork** — handmade DarkFrame glyphs that override the generated result for apps we
   have drawn. A quality mechanism, not a coverage mechanism.

Wallpapers, widgets and complete styles remain local catalog items. Collections are data-driven, so
adding one is a data change rather than a navigation rewrite.

## Monetization

FREE and PRO are domain-level tiers. V1 defaults to FREE until a real Google Play Billing entitlement
provider is wired; `LocalFreeEntitlementProvider` never grants fake Pro. Three of six collections are
free, and the dynamic engine itself is not gated — a free user themes their entire device.

## Android boundaries

DarkFrame never promises silent system customization. Wallpaper APIs and launcher-supported icon
application are automated only where Android permits; everything else gets guided setup that names the
actual constraint. The capability model is data (`LauncherCapabilityTable`) and the guided-setup screen
is driven by it, so the product is structurally unable to describe a mechanism it cannot perform.

See `docs/LAUNCHER_SUPPORT.md` and `docs/PACKAGE_VISIBILITY.md`.

## Layering

The engine is split so that the rules are testable without a device:

- **domain** — no Android imports. Identity, ordering, collections, normalisation maths, colour maths,
  cache identity, curated lookup, launcher capability. Covered by JVM unit tests.
- **data** — platform queries, resource parsing, cache storage, package broadcasts.
- **render** — bitmap work. The only layer that touches `Canvas`.
- **apply** — the three real apply mechanisms.
- **ui** — wiring and state only; receives finished bitmaps.

Dependencies point inward: `ui → engine → data/render → domain`. Nothing in `domain` knows the rest
exists, which is what keeps the 76 unit tests fast and device-free.

## Fold and large screens

Phone, cover-screen and expanded layouts are validated independently. Large screens get adapted content
density rather than stretched phone UI, and the icon grid recomputes its span count on layout so a
fold/unfold is handled without recreating the activity.
