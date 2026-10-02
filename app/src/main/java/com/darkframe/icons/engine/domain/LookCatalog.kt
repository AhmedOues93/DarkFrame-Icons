package com.darkframe.icons.engine.domain

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.IconCollection
import com.darkframe.icons.model.WidgetKind

/**
 * The six DarkFrame looks.
 *
 * One per collection, by design: a user picking "Noir" should get Noir icons, the wallpaper Noir was
 * composed against and the widgets that suit it, without having to assemble that themselves. The
 * tier matches the collection's, so a look is never half-locked.
 */
object LookCatalog {

    val noir = CompleteLook(
        id = "noir",
        name = "Noir",
        tagline = "Deep black, warm white glyphs, nothing else.",
        collection = IconCollection.NOIR,
        wallpaperId = "carbon_weave",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.BATTERY),
        palette = LookPalette(surface = 0xFF0C0D10L, ink = 0xFFF2F1EEL, accent = 0xFF7E8691L),
        layout = LayoutRecommendation(
            columns = 4,
            rows = 5,
            labels = false,
            note = "Four columns and no labels: Noir's whole point is the icons' own silhouettes, " +
                "and a row of white captions under them is the thing that breaks it.",
        ),
        tier = ContentTier.FREE,
    )

    val colorPop = CompleteLook(
        id = "color_pop",
        name = "Color Pop",
        tagline = "Your apps' own colours, on a clean neutral ground.",
        collection = IconCollection.COLOR_POP,
        wallpaperId = "graphite_wash",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.DATE),
        // Muted on purpose, and the most important accent in the catalog to get right: in Color Pop
        // the colour comes from the user's own app icons, so DarkFrame's own accent has to stay out
        // of their way. The first value here was a visible blue that competed with them.
        palette = LookPalette(surface = 0xFF121418L, ink = 0xFFF4F2EEL, accent = 0xFF8795A3L),
        layout = LayoutRecommendation(
            columns = 4,
            rows = 5,
            labels = true,
            note = "Labels on: Color Pop keeps each app recognisable, so the name beside the colour " +
                "costs nothing and makes a dense grid faster to read.",
        ),
        tier = ContentTier.FREE,
    )

    val pureAmoled = CompleteLook(
        id = "pure_amoled",
        name = "Pure AMOLED",
        tagline = "True black. The panel switches pixels off.",
        collection = IconCollection.PURE_AMOLED,
        wallpaperId = "amoled_void",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.BATTERY),
        palette = LookPalette(surface = 0xFF000000L, ink = 0xFFFFFFFFL, accent = 0xFF6E747EL),
        layout = LayoutRecommendation(
            columns = 5,
            rows = 6,
            labels = false,
            note = "Denser than the rest, and no labels. On true black the gaps are the design, so " +
                "more icons and less text means more of them.",
        ),
        tier = ContentTier.FREE,
    )

    val frost = CompleteLook(
        id = "frost",
        name = "Frost",
        tagline = "The light one. Off-white surfaces, graphite ink.",
        collection = IconCollection.FROST,
        wallpaperId = "frost_panes",
        widgets = listOf(WidgetKind.DATE, WidgetKind.CALENDAR),
        palette = LookPalette(surface = 0xFFF4F3EFL, ink = 0xFF23262BL, accent = 0xFF8C9199L),
        layout = LayoutRecommendation(
            columns = 4,
            rows = 5,
            labels = true,
            note = "Labels on. Dark text on a light ground is the most legible combination DarkFrame " +
                "has, and Frost is the only look that gets it.",
        ),
        tier = ContentTier.PRO,
    )

    val titanium = CompleteLook(
        id = "titanium",
        name = "Titanium",
        tagline = "Brushed graphite and silver, with no colour cast.",
        collection = IconCollection.TITANIUM,
        wallpaperId = "titanium_brushed",
        widgets = listOf(WidgetKind.ANALOG_CLOCK, WidgetKind.INFO),
        palette = LookPalette(surface = 0xFF1C1F24L, ink = 0xFFE4E7EBL, accent = 0xFF99A1ACL),
        layout = LayoutRecommendation(
            columns = 4,
            rows = 5,
            labels = false,
            note = "No labels, and a wallpaper with a direction to it: the brushed sweep reads as one " +
                "surface, and captions cut it into rows.",
        ),
        tier = ContentTier.PRO,
    )

    val glass = CompleteLook(
        id = "glass",
        name = "Glass",
        tagline = "One highlight, one edge. Restraint as a material.",
        collection = IconCollection.GLASS,
        wallpaperId = "glass_panes",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.DATE),
        palette = LookPalette(surface = 0xFF0D1014L, ink = 0xFFF0F3F6L, accent = 0xFF7FA3BEL),
        layout = LayoutRecommendation(
            columns = 4,
            rows = 5,
            labels = false,
            note = "Keep it sparse. Glass depends on what shows through it, so leaving a column of " +
                "space does more for the look than filling the grid.",
        ),
        tier = ContentTier.PRO,
    )

    /** Free looks first, so the first thing a new user sees is something they can actually use. */
    val all: List<CompleteLook> = listOf(noir, colorPop, pureAmoled, frost, titanium, glass)

    private val byId = all.associateBy { it.id }
    private val byCollection = all.associateBy { it.collection }

    fun forId(id: String?): CompleteLook = byId[id] ?: default

    fun forCollection(collection: IconCollection): CompleteLook = byCollection.getValue(collection)

    val default: CompleteLook get() = noir
}
