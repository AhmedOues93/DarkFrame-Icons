# Curated artwork plan

## What this plan is not

It is **not** a coverage plan. Coverage is already complete: the dynamic engine themes every app the
device exposes, including apps nobody has drawn and apps that do not exist yet. Adding curated
artwork does not increase the number of apps DarkFrame supports, because that number is already
"all of them".

What curated artwork buys is **quality on the icons people look at most**. A generated icon is a
normalised, restyled version of the app's own artwork — good, consistent, and clearly DarkFrame. A
handmade glyph is better: it is drawn on DarkFrame's own stroke language, at DarkFrame's own optical
weight, and it reads correctly in all six collections because it carries no background of its own.

So the priority order below is a priority order for *polish*, measured by how often a glyph appears
on a home screen.

## Authoring rules

- Glyph on transparency, no background: the collection supplies the container. One drawing serves
  all six collections.
- White as the authoring colour. The engine maps source luma onto the collection tint, so pure white
  lands exactly on the tint and interior shading survives.
- 108 viewport, consistent stroke weight, legible at 48px.
- Original artwork. A trademark may be referenced only as far as needed to identify the app; no
  third-party icon-pack artwork is reused.
- Registered in `res/xml/df_curated.xml`, preferring `package=` over `component=` — an app's launcher
  activity name changes between releases far more often than its package name, and a stale component
  entry silently stops matching.

## Priority order

1. Google and Samsung system apps (present on nearly every device in the target market)
2. Messaging and social: WhatsApp, Instagram, TikTok, YouTube, Telegram, Snapchat, X, Reddit, Discord
3. Media and shopping: Spotify, Netflix, Amazon, PayPal
4. Microsoft and productivity
5. AI and developer tools: ChatGPT, Claude, Gemini, GitHub
6. German and EU banking, transport, shopping and media
7. Long tail, driven by icon requests — which, importantly, are requests for *better* artwork, not
   requests for support. An app that is not in this list already works.

## Current state

12 curated glyphs (`dfg_*`), covering the launch set from the original Classic Outline series.

The original `df_*` composed tiles are retained and serve a different purpose: they are the artwork
third-party launchers draw through `res/xml/appfilter.xml`, where DarkFrame cannot compose at runtime.
See `docs/LAUNCHER_SUPPORT.md` for why those two files are not duplicates.

## How to tell whether this plan is working

The browser reports both numbers — apps themed, and apps with DarkFrame artwork. The first is the
product's coverage and should equal the user's app drawer. The second is this plan's progress. They
are deliberately shown together so the distinction stays visible internally as well as to the user.
