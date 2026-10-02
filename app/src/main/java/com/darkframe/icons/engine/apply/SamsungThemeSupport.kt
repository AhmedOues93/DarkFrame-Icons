package com.darkframe.icons.engine.apply

/**
 * What DarkFrame can and cannot do on Samsung One UI.
 *
 * ## The constraint
 *
 * One UI Home does not read third-party icon packs. Samsung themes icons through its own theming
 * stack, and the piece a normal app can reach is **Theme Park** — a Good Lock module Samsung ships
 * through the Galaxy Store, which lets a user build an icon theme from an installed icon pack and
 * apply it system-wide.
 *
 * That means the final apply step belongs to Samsung's app, not to DarkFrame, and no API lets us
 * perform it on the user's behalf. What DarkFrame *can* do is make its icon pack discoverable in the
 * standard format Theme Park reads, detect whether Theme Park is installed, and hand over in one tap.
 *
 * ## Why not pinned shortcuts
 *
 * An earlier build used [ShortcutManagerCompat.requestPinShortcut] as the Samsung path. Testing on a
 * real Galaxy Z Fold8 showed what that actually produces: a *second* icon beside the original, not a
 * replacement. The user ends up with duplicates and an unchanged app drawer. It is a legitimate API
 * and a bad product, so it is no longer the Samsung flow — see [SamsungApplyStep].
 */
object SamsungThemeSupport {

    /** Launcher package of Samsung One UI Home. */
    const val ONE_UI_HOME = "com.sec.android.app.launcher"

    /** Theme Park, the Good Lock module that builds and applies icon themes. */
    const val THEME_PARK = "com.samsung.android.themedesigner"

    /** Good Lock itself, from which Theme Park is installed. */
    const val GOOD_LOCK = "com.samsung.app.goodlock"

    /** Older Good Lock package name, still present on some devices. */
    const val GOOD_LOCK_LEGACY = "com.samsung.android.goodlock"

    /** Galaxy Store, where Good Lock and Theme Park come from. */
    const val GALAXY_STORE = "com.sec.android.app.samsungapps"

    /** Galaxy Store deep link for a package. */
    fun galaxyStoreUri(packageName: String): String =
        "samsungapps://ProductDetail/$packageName"

    /** Play Store fallback for devices where Galaxy Store is unavailable. */
    fun playStoreUri(packageName: String): String = "market://details?id=$packageName"

    /**
     * True for a home launcher that is Samsung's own.
     *
     * Matched on the package rather than on the manufacturer: what matters is which launcher is
     * actually drawing the home screen, and a Galaxy device running Nova should get the icon-pack
     * flow, not the Samsung one.
     */
    fun isOneUiHome(launcherPackage: String?): Boolean = launcherPackage == ONE_UI_HOME

    /**
     * The step the user is actually on, which is what the UI should offer them.
     *
     * Deliberately a small closed set: every value corresponds to an action DarkFrame can really
     * perform, so the screen cannot end up showing a button with nothing behind it.
     */
    fun stepFor(themeParkInstalled: Boolean, goodLockInstalled: Boolean): SamsungApplyStep = when {
        themeParkInstalled -> SamsungApplyStep.OPEN_THEME_PARK
        goodLockInstalled -> SamsungApplyStep.OPEN_GOOD_LOCK
        else -> SamsungApplyStep.INSTALL_GOOD_LOCK
    }
}

/** A step DarkFrame can actually carry out on a Samsung device. */
enum class SamsungApplyStep {
    /** Theme Park is installed: hand straight over to it. */
    OPEN_THEME_PARK,

    /** Good Lock is installed but Theme Park is not: open Good Lock so the user can add it. */
    OPEN_GOOD_LOCK,

    /** Neither is installed: send the user to the store listing. */
    INSTALL_GOOD_LOCK,
}
