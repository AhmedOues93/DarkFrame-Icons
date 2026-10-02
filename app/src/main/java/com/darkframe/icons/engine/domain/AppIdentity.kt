package com.darkframe.icons.engine.domain

/**
 * A single launchable entry point of an installed application.
 *
 * DarkFrame is component-addressed rather than package-addressed: a package may publish more
 * than one launcher activity (Samsung Secure Folder clones, dual-mode camera apps, games with
 * separate editor entry points). Each of those is its own [AppIdentity] because a launcher can
 * show, and theme, each one independently.
 *
 * This type is deliberately free of Android framework references so the resolution, caching and
 * ordering rules around it stay unit-testable on the JVM.
 */
data class AppIdentity(
    val packageName: String,
    val activityName: String,
    /** User-visible activity label, already resolved against the current locale. */
    val label: String,
    /**
     * Opaque change stamp for the installed package — `lastUpdateTime` in practice. It only has
     * to change when the installed icon could have changed, because it is part of the cache key.
     */
    val versionStamp: Long,
    /** True when the owning package publishes more than one launcher activity. */
    val isSecondaryEntryPoint: Boolean = false,
    /** True for entries that belong to a managed/secondary profile such as Secure Folder. */
    val isWorkProfile: Boolean = false,
) {
    /** Stable identifier used for cache keys, curated lookups and list diffing. */
    val componentKey: String get() = "$packageName/$activityName"

    /** The `ComponentInfo{pkg/activity}` form used by the icon-pack `appfilter.xml` convention. */
    val flattenedComponent: String get() = "ComponentInfo{$packageName/$activityName}"

    /**
     * Label used in the browser. Secondary entry points are disambiguated so the user does not
     * see two visually identical rows.
     */
    fun displayLabel(): String = when {
        isWorkProfile -> "$label (work)"
        isSecondaryEntryPoint -> "$label · ${activityName.substringAfterLast('.')}"
        else -> label
    }

    companion object {
        /** Parses the `ComponentInfo{pkg/activity}` form. Returns null for anything malformed. */
        fun parseFlattened(value: String): Pair<String, String>? {
            val trimmed = value.trim()
            if (!trimmed.startsWith("ComponentInfo{") || !trimmed.endsWith("}")) return null
            val body = trimmed.substring("ComponentInfo{".length, trimmed.length - 1)
            val slash = body.indexOf('/')
            if (slash <= 0 || slash == body.length - 1) return null
            val pkg = body.substring(0, slash)
            val activity = body.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
            return pkg to activity
        }
    }
}
