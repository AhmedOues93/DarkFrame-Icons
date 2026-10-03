package com.darkframe.icons.engine.domain

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.IconCollection

/**
 * The six DarkFrame collections as data.
 *
 * All six share one optical grid: the same corner geometry, the same keyline weight scale and a
 * glyph size band narrow enough that switching collections never changes how large icons *feel*
 * on the home screen. What changes between them is surface and glyph treatment only.
 *
 * These values are DarkFrame's own; they are not derived from any third-party icon pack.
 */
object IconStyleCatalog {

    /** Shared corner geometry for every collection — one of DarkFrame's identity constants. */
    const val CORNER_RADIUS_RATIO = 0.225f

    /** Shared keyline weight. Hairline at every size because it is ratio-based. */
    const val KEYLINE_WIDTH_RATIO = 0.0075f

    val noir = IconStyle(
        id = "noir",
        collection = IconCollection.NOIR,
        displayName = "Noir",
        description = "Luxury monochrome. Deep-black container, warm-white glyph, hairline keyline.",
        tier = ContentTier.FREE,
        containerColor = 0xFF0B0C0FL,
        containerEndColor = 0xFF0B0C0FL,
        finish = SurfaceFinish.FLAT,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        keylineColor = 0x1AFFFFFFL,
        keylineWidthRatio = KEYLINE_WIDTH_RATIO,
        glyphMode = GlyphMode.TINT,
        glyphTint = 0xFFF2F1EEL,
        glyphSaturation = 0f,
        glyphScale = 0.52f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = true,
    )

    val colorPop = IconStyle(
        id = "color_pop",
        collection = IconCollection.COLOR_POP,
        displayName = "Color Pop",
        description = "Original brand colours at full clarity on a clean neutral container.",
        tier = ContentTier.FREE,
        containerColor = 0xFF17191DL,
        containerEndColor = 0xFF17191DL,
        finish = SurfaceFinish.FLAT,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        keylineColor = 0x14FFFFFFL,
        keylineWidthRatio = KEYLINE_WIDTH_RATIO,
        glyphMode = GlyphMode.PRESERVE,
        // Held just under 1.0: enough to stop over-saturated source artwork reading as neon,
        // not enough to make a brand colour look wrong next to its own app.
        glyphSaturation = 0.94f,
        glyphTint = 0x00000000L,
        glyphScale = 0.56f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = false,
    )

    val frost = IconStyle(
        id = "frost",
        collection = IconCollection.FROST,
        displayName = "Frost",
        description = "Premium light finish. Off-white container with graphite glyphs.",
        tier = ContentTier.FREE,
        containerColor = 0xFFF2F1EDL,
        containerEndColor = 0xFFF2F1EDL,
        finish = SurfaceFinish.FLAT,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        keylineColor = 0x1A101114L,
        keylineWidthRatio = KEYLINE_WIDTH_RATIO,
        glyphMode = GlyphMode.TINT,
        glyphTint = 0xFF23262BL,
        glyphSaturation = 0f,
        glyphScale = 0.52f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = true,
    )

    val titanium = IconStyle(
        id = "titanium",
        collection = IconCollection.TITANIUM,
        displayName = "Titanium",
        description = "Neutral graphite-to-silver metal. Low-contrast sweep, no colour cast.",
        tier = ContentTier.FREE,
        containerColor = 0xFF2A2D32L,
        containerEndColor = 0xFF474B52L,
        finish = SurfaceFinish.METALLIC,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        keylineColor = 0x26E8EAEEL,
        keylineWidthRatio = KEYLINE_WIDTH_RATIO,
        glyphMode = GlyphMode.TINT,
        glyphTint = 0xFFE4E7EBL,
        glyphSaturation = 0f,
        glyphScale = 0.50f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = true,
    )

    val glass = IconStyle(
        id = "glass",
        collection = IconCollection.GLASS,
        displayName = "Glass",
        description = "Restrained translucency: one highlight, one inner edge, nothing more.",
        tier = ContentTier.FREE,
        containerColor = 0xCC14171BL,
        containerEndColor = 0xCC14171BL,
        finish = SurfaceFinish.GLASS,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        keylineColor = 0x33FFFFFFL,
        keylineWidthRatio = KEYLINE_WIDTH_RATIO,
        glyphMode = GlyphMode.PRESERVE,
        glyphSaturation = 0.90f,
        glyphTint = 0x00000000L,
        glyphScale = 0.54f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = false,
    )

    val pureAmoled = IconStyle(
        id = "pure_amoled",
        collection = IconCollection.PURE_AMOLED,
        displayName = "Pure AMOLED",
        description = "True black, no keyline, maximum contrast. Pixels stay switched off.",
        tier = ContentTier.FREE,
        containerColor = 0xFF000000L,
        containerEndColor = 0xFF000000L,
        finish = SurfaceFinish.FLAT,
        cornerRadiusRatio = CORNER_RADIUS_RATIO,
        // Fully transparent: a keyline would defeat the point of a true-black tile.
        keylineColor = 0x00000000L,
        keylineWidthRatio = 0f,
        glyphMode = GlyphMode.TINT,
        glyphTint = 0xFFFFFFFFL,
        glyphSaturation = 0f,
        glyphScale = 0.58f,
        opticalLiftRatio = 0f,
        preferMonochromeLayer = true,
    )

    /** Catalog order is the order the UI presents collections in. */
    val all: List<IconStyle> = listOf(noir, colorPop, frost, titanium, glass, pureAmoled)

    private val byCollection: Map<IconCollection, IconStyle> = all.associateBy { it.collection }
    private val byId: Map<String, IconStyle> = all.associateBy { it.id }

    /** Every [IconCollection] is backed by exactly one style, so this never fails. */
    fun forCollection(collection: IconCollection): IconStyle =
        byCollection.getValue(collection)

    /** Falls back to [default] for unknown ids so a stale persisted preference cannot crash. */
    fun forId(id: String?): IconStyle = byId[id] ?: default

    val default: IconStyle get() = noir
}
