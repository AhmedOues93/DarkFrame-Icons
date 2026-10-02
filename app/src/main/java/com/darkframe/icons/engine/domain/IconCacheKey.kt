package com.darkframe.icons.engine.domain

/**
 * Cache identity for one rendered icon.
 *
 * Correct invalidation is the whole job here. A cached bitmap is only valid for an exact
 * combination of: which component it is, which build of that component is installed, whether a
 * curated override was used, which style was applied, which renderer produced it, and at what
 * pixel size. Every one of those is in the key, so there is no scenario where a stale bitmap can
 * be served — a mismatch simply misses the cache and re-renders.
 *
 * In particular, [AppIdentity.versionStamp] in the key is what makes app *updates* correct: an
 * updated app gets a new stamp, so its old entries become unreachable and age out, and the new
 * icon is picked up on the next read without any explicit invalidation call.
 */
object IconCacheKey {

    /** Logical key. Human-readable so cache behaviour is debuggable from a log line. */
    fun of(
        identity: AppIdentity,
        style: IconStyle,
        sizePx: Int,
        curated: Boolean,
    ): String = buildString {
        append("v").append(IconStyle.RENDER_VERSION)
        append('|').append(style.id)
        append('|').append(sizePx)
        append('|').append(if (curated) "c" else "o")
        append('|').append(identity.componentKey)
        append('@').append(identity.versionStamp)
    }

    /**
     * Filesystem-safe disk name for a logical key. Hashed rather than sanitised because package
     * and activity names routinely exceed filename length limits once concatenated.
     */
    fun diskName(key: String): String = "${StableHash.hex(key)}.png"

    /**
     * Directory shard for a package. Rendered icons for one package all live together so that
     * uninstalling it is a single recursive delete rather than a scan of every cached file.
     */
    fun packageShard(packageName: String): String = StableHash.hex(packageName)
}

/**
 * FNV-1a 64-bit.
 *
 * Deliberately not [String.hashCode]: that is 32-bit (collisions are reachable across a few
 * thousand apps x six styles x several sizes) and its value is not contractually stable across
 * JVM versions, whereas these hashes are written to disk and must still match on the next launch.
 */
object StableHash {
    private const val OFFSET_BASIS = -3750763034362895579L // 0xcbf29ce484222325
    private const val PRIME = 1099511628211L

    fun of(value: String): Long {
        var hash = OFFSET_BASIS
        for (char in value) {
            hash = hash xor (char.code.toLong() and 0xFFFFL)
            hash *= PRIME
        }
        return hash
    }

    fun hex(value: String): String = java.lang.Long.toHexString(of(value)).padStart(16, '0')
}
