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
    val tier: ContentTier,
) {
    /** The icon style this look uses. */
    val style: IconStyle get() = IconStyleCatalog.forCollection(collection)
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
