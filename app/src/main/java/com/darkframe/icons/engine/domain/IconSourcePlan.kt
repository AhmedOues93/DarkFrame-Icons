package com.darkframe.icons.engine.domain

/** Where the artwork for an icon comes from. */
enum class IconSourceKind {
    /** DarkFrame's own handmade glyph. */
    CURATED,

    /** The app's own installed icon — the normal case for most apps on any device. */
    INSTALLED,

    /** Styled initials, for an app whose icon cannot be loaded at all. */
    MONOGRAM,
}

/**
 * The engine's central rule, as data.
 *
 * This is the single most important behaviour in the product, so it is a pure function with tests
 * rather than something emergent from the order of `if` statements in a loader:
 *
 *  - curated artwork is tried **first**, because when we have drawn something it is better;
 *  - the app's own installed icon is **always** the next candidate, which is what makes DarkFrame
 *    work for apps nobody has drawn and for apps that do not exist yet;
 *  - a monogram is the last resort, so a broken install cannot leave a hole in the grid.
 *
 * Expressed as a preference *order* rather than a single choice because usability is only known by
 * trying: a curated row can name artwork missing from the APK, and an installed icon can fail to
 * load. Each candidate falls through to the next, so a failure costs polish and never coverage.
 */
object IconSourcePlan {

    /**
     * Candidates in order of preference. Always ends with [IconSourceKind.MONOGRAM], so the list is
     * never exhausted without a result.
     */
    fun preferenceOrder(hasCuratedOverride: Boolean): List<IconSourceKind> =
        if (hasCuratedOverride) {
            listOf(IconSourceKind.CURATED, IconSourceKind.INSTALLED, IconSourceKind.MONOGRAM)
        } else {
            listOf(IconSourceKind.INSTALLED, IconSourceKind.MONOGRAM)
        }
}
