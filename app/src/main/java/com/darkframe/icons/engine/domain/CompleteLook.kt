package com.darkframe.icons.engine.domain

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.IconCollection
import com.darkframe.icons.model.WidgetKind

/**
 * One finished DarkFrame look: what the phone will look like, as a single thing the user chooses.
 *
 * A look is the product's unit of choice. The collection, the wallpaper and the widgets are not
 * three unrelated settings the user has to match up themselves — they are picked together so the
 * result is coherent, and the home screen can show one preview of all of it before anything is
 * applied.
 *
 * Each part also carries how it gets applied, which is not the same for all three:
 *  - **wallpaper** DarkFrame can set directly, with the user's permission;
 *  - **icons** need the launcher's cooperation (Samsung Theme Park, or an icon-pack launcher);
 *  - **widgets** can only be placed by the user, because Android has no API for placing one.
 *
 * Those differences are a property of Android, so they live in the model rather than being glossed
 * over in the UI.
 */
data class CompleteLook(
    val id: String,
    val name: String,
    /** One line, shown under the name. Describes the look, not the mechanism. */
    val tagline: String,
    val collection: IconCollection,
    val wallpaperId: String,
    val widgets: List<WidgetKind>,
    val palette: LookPalette,
    val layout: LayoutRecommendation,
    val tier: ContentTier,
) {
    /** The icon style this look uses. */
    val style: IconStyle get() = IconStyleCatalog.forCollection(collection)
}

/**
 * The three colours a look is built from, as the user sees them.
 *
 * Stated per look rather than read off the icon style, because the two are not the same thing: the
 * style's container is the colour under a *glyph*, and a look's surface is the colour the whole home
 * screen sits on, which comes from its wallpaper. Showing the real three is what makes a look
 * something a user can judge against their own taste before applying any of it.
 */
data class LookPalette(
    /** The ground: what the wallpaper and the icon containers settle to. */
    val surface: Long,
    /** The ink: glyphs, widget text, labels. */
    val ink: Long,
    /** The one colour allowed to stand out. Kept quiet in every look; DarkFrame is not a bright pack. */
    val accent: Long,
) {
    val swatches: List<Long> get() = listOf(surface, ink, accent)
}

/**
 * How to arrange a home screen so a look reads the way it was composed.
 *
 * Advice, not an action. Android exposes no way for one app to set another launcher's grid density
 * or turn its labels off, and One UI's own grid setting lives in its home-screen settings — so this
 * is written as a recommendation the user can follow in a few taps, and the UI never pretends to
 * apply it. Being explicit about that is the same principle as the widgets screen: say where the
 * capability lives.
 */
data class LayoutRecommendation(
    val columns: Int,
    val rows: Int,
    /** Whether the look was composed with app labels showing. */
    val labels: Boolean,
    /** One line of why, so the recommendation is reasoning rather than a decree. */
    val note: String,
) {
    init {
        require(columns in 3..8) { "columns out of range" }
        require(rows in 3..8) { "rows out of range" }
    }

    /** "4 x 5", the way One UI's own home-screen setting states it. */
    val grid: String get() = "$columns x $rows"
}

/** How much of a look DarkFrame can put in place by itself. */
enum class LookApplyPart {
    /** Set directly through WallpaperManager. */
    WALLPAPER,

    /** Handed to the launcher: Samsung Theme Park, or an icon-pack launcher's own setting. */
    ICONS,

    /** The user places these; Android has no API for adding a widget to a home screen. */
    WIDGETS,
}

/**
 * What a look's apply flow consists of.
 *
 * Deliberately explicit about which parts are automatic, because the alternative — an "Apply" button
 * that silently does one third of what it says — is the thing this product must not do.
 */
object LookApplyPlan {

    /** Parts DarkFrame performs itself once the user confirms. */
    fun automaticParts(): Set<LookApplyPart> = setOf(LookApplyPart.WALLPAPER)

    /** Parts that need another app or the user to finish. */
    fun guidedParts(): Set<LookApplyPart> = setOf(LookApplyPart.ICONS, LookApplyPart.WIDGETS)

    /** Every part is accounted for in exactly one of the two sets. */
    fun allParts(): Set<LookApplyPart> = automaticParts() + guidedParts()
}
