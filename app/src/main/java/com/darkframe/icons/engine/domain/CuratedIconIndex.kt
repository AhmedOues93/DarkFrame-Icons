package com.darkframe.icons.engine.domain

/**
 * One curated-override declaration, as read from `res/xml/appfilter.xml`.
 *
 * Exactly one of [component] / [packageName] is meaningful: a component entry overrides a single
 * launcher activity, a package entry overrides every activity of a package.
 */
data class CuratedEntry(
    val drawableName: String,
    val component: String? = null,
    val packageName: String? = null,
)

/**
 * Lookup for DarkFrame's handmade icon overrides.
 *
 * This is explicitly **not** a supported-app list. The engine themes every installed app whether
 * or not it appears here; an entry only means "we have drawn something better than the installed
 * icon for this one, prefer it". An empty index is a perfectly valid state in which DarkFrame
 * still themes the user's entire app drawer.
 *
 * Resolution order is most-specific-first: exact launcher component, then package-wide.
 */
class CuratedIconIndex private constructor(
    private val byComponent: Map<String, String>,
    private val byPackage: Map<String, String>,
) {

    val size: Int get() = byComponent.size + byPackage.size
    val isEmpty: Boolean get() = size == 0

    /** Drawable resource *name* of the curated override, or null when there is none. */
    fun drawableNameFor(identity: AppIdentity): String? =
        byComponent[identity.componentKey] ?: byPackage[identity.packageName]

    fun hasOverrideFor(identity: AppIdentity): Boolean = drawableNameFor(identity) != null

    /** Every distinct curated drawable name, for the "curated artwork" browser filter. */
    fun drawableNames(): Set<String> = byComponent.values.toSet() + byPackage.values.toSet()

    companion object {
        val EMPTY = CuratedIconIndex(emptyMap(), emptyMap())

        /**
         * Builds an index, skipping entries that cannot be used. Malformed `appfilter.xml` rows
         * are dropped rather than thrown on: a single bad row in a large curated set must not
         * take down icon resolution for every app on the device.
         */
        fun from(entries: List<CuratedEntry>): CuratedIconIndex {
            val components = LinkedHashMap<String, String>()
            val packages = LinkedHashMap<String, String>()
            for (entry in entries) {
                if (entry.drawableName.isBlank()) continue
                val component = entry.component?.takeIf { it.isNotBlank() }
                if (component != null) {
                    val parsed = parseComponent(component) ?: continue
                    components["${parsed.first}/${parsed.second}"] = entry.drawableName
                    continue
                }
                val pkg = entry.packageName?.takeIf { it.isNotBlank() } ?: continue
                packages[pkg] = entry.drawableName
            }
            return CuratedIconIndex(components, packages)
        }

        /**
         * Accepts the plain `pkg/activity` form as well as `ComponentInfo{...}`.
         *
         * The two forms are dispatched on, never tried in sequence: falling back from a failed
         * `ComponentInfo{...}` parse to the bare parser makes a malformed wrapper such as
         * `ComponentInfo{com.a/}` parse "successfully" into the nonsense package
         * `ComponentInfo{com.a`, which would then silently never match anything.
         */
        private fun parseComponent(value: String): Pair<String, String>? {
            val trimmed = value.trim()
            return if (trimmed.startsWith("ComponentInfo{")) {
                AppIdentity.parseFlattened(trimmed)
            } else {
                parseBareComponent(trimmed)
            }
        }

        private fun parseBareComponent(value: String): Pair<String, String>? {
            val slash = value.indexOf('/')
            if (slash <= 0 || slash == value.length - 1) return null
            val pkg = value.substring(0, slash)
            val activity = value.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
            return pkg to activity
        }
    }
}
