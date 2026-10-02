package com.darkframe.icons.engine.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.IconCacheKey
import java.io.File

/**
 * Two-level cache for rendered icons: an in-memory LRU in front of a disk store.
 *
 * Rendering an icon costs a package-manager call, a rasterise, an alpha scan and a composite. At
 * a few hundred apps times six collections that is far too much to redo on every screen, so results
 * are kept — but only ever under a key that fully describes them (see [IconCacheKey]).
 *
 * ## Invalidation
 *
 * Most invalidation is structural rather than explicit, which is the only way to make it reliable:
 *  - **App updated** — the key contains the package's `lastUpdateTime`, so an updated app's old
 *    entries simply become unreachable. No signal has to arrive for correctness.
 *  - **Renderer or style changed** — the key contains [com.darkframe.icons.engine.domain.IconStyle.RENDER_VERSION].
 *  - **App uninstalled** — [invalidatePackage] drops that package's shard. Packages are sharded by
 *    directory precisely so this is one recursive delete instead of a scan of every cached file.
 *  - **Everything** — [clear], for a user-initiated "rebuild icon cache".
 *
 * Unreachable files are reclaimed by [trimToBudget] rather than hunted down individually.
 */
class IconCache(
    context: Context,
    memoryBudgetBytes: Int = defaultMemoryBudget(),
    private val diskBudgetBytes: Long = DEFAULT_DISK_BUDGET_BYTES,
) {

    private val root = File(context.cacheDir, "df_icons")

    private val memory = object : LruCache<String, Bitmap>(memoryBudgetBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Guards disk layout mutations. Reads and writes of distinct files do not contend. */
    private val diskLock = Any()

    fun fromMemory(key: String): Bitmap? = memory.get(key)

    @WorkerThread
    fun get(key: String, packageName: String): Bitmap? {
        memory.get(key)?.let { return it }
        val file = fileFor(key, packageName)
        if (!file.exists()) return null
        val decoded = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        if (decoded == null) {
            // A truncated or corrupt file (killed mid-write on an older build, bad sector) is
            // removed so the next read re-renders instead of failing forever.
            runCatching { file.delete() }
            return null
        }
        memory.put(key, decoded)
        return decoded
    }

    @WorkerThread
    fun put(key: String, packageName: String, bitmap: Bitmap) {
        memory.put(key, bitmap)
        val file = fileFor(key, packageName)
        runCatching {
            synchronized(diskLock) { file.parentFile?.mkdirs() }
            // Written to a temp file and renamed, so a kill mid-write leaves the old entry or
            // nothing — never a half-decoded icon.
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (!temp.renameTo(file)) {
                temp.delete()
            }
        }
    }

    /** Called when a package is removed, replaced or disabled. */
    @WorkerThread
    fun invalidatePackage(packageName: String) {
        val shard = IconCacheKey.packageShard(packageName)
        synchronized(diskLock) {
            runCatching { File(root, shard).deleteRecursively() }
        }
        // The memory LRU is not keyed by package, and walking it to evict one app's entries is
        // not worth it: those entries are already unreachable by key after a reinstall or update,
        // and the LRU evicts them in due course.
        memory.evictAll()
    }

    @WorkerThread
    fun clear() {
        memory.evictAll()
        synchronized(diskLock) {
            runCatching { root.deleteRecursively() }
        }
    }

    /** Current on-disk size in bytes. */
    @WorkerThread
    fun diskUsageBytes(): Long = runCatching {
        root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)

    /**
     * Evicts least-recently-used files until the store fits [diskBudgetBytes].
     *
     * Worth running after a bulk render (opening the browser, switching collection) rather than on
     * every write, which would turn each icon into a full directory walk.
     */
    @WorkerThread
    fun trimToBudget() {
        runCatching {
            synchronized(diskLock) {
                val files = root.walkTopDown().filter { it.isFile }.toMutableList()
                var total = files.sumOf { it.length() }
                if (total <= diskBudgetBytes) return@synchronized
                files.sortBy { it.lastModified() }
                for (file in files) {
                    if (total <= diskBudgetBytes) break
                    val size = file.length()
                    if (file.delete()) total -= size
                }
            }
        }
    }

    private fun fileFor(key: String, packageName: String): File =
        File(File(root, IconCacheKey.packageShard(packageName)), IconCacheKey.diskName(key))

    companion object {
        /** 24 MiB of disk: roughly a thousand icons at browser resolution. */
        const val DEFAULT_DISK_BUDGET_BYTES = 24L * 1024 * 1024

        /**
         * An eighth of the heap. Icon bitmaps are the app's dominant allocation, but a cache that
         * crowds out the rest of the process trades a scroll stutter for an OOM.
         */
        fun defaultMemoryBudget(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory()
            val eighth = (maxMemory / 8).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            return eighth.coerceAtLeast(4 * 1024 * 1024)
        }
    }
}
