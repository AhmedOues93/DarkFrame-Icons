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
        tier = ContentTier.FREE,
    )

    val colorPop = CompleteLook(
        id = "color_pop",
        name = "Color Pop",
        tagline = "Your apps' own colours, on a clean neutral ground.",
        collection = IconCollection.COLOR_POP,
        wallpaperId = "graphite_wash",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.DATE),
        tier = ContentTier.FREE,
    )

    val pureAmoled = CompleteLook(
        id = "pure_amoled",
        name = "Pure AMOLED",
        tagline = "True black. The panel switches pixels off.",
        collection = IconCollection.PURE_AMOLED,
        wallpaperId = "amoled_void",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.BATTERY),
        tier = ContentTier.FREE,
    )

    val frost = CompleteLook(
        id = "frost",
        name = "Frost",
        tagline = "The light one. Off-white surfaces, graphite ink.",
        collection = IconCollection.FROST,
        wallpaperId = "minimal_paper",
        widgets = listOf(WidgetKind.DATE, WidgetKind.CALENDAR),
        tier = ContentTier.PRO,
    )

    val titanium = CompleteLook(
        id = "titanium",
        name = "Titanium",
        tagline = "Brushed graphite and silver, with no colour cast.",
        collection = IconCollection.TITANIUM,
        wallpaperId = "titanium_brushed",
        widgets = listOf(WidgetKind.ANALOG_CLOCK, WidgetKind.INFO),
        tier = ContentTier.PRO,
    )

    val glass = CompleteLook(
        id = "glass",
        name = "Glass",
        tagline = "One highlight, one edge. Restraint as a material.",
        collection = IconCollection.GLASS,
        wallpaperId = "glass_panes",
        widgets = listOf(WidgetKind.DIGITAL_CLOCK, WidgetKind.DATE),
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
