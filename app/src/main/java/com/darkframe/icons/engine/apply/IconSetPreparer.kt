package com.darkframe.icons.engine.apply

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import com.darkframe.icons.engine.IconResolver
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconRenderPolicy
import com.darkframe.icons.engine.domain.IconStyle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File

/** How far a prepare run got. Reported as it goes so the UI can show real progress. */
data class PrepareProgress(val done: Int, val total: Int) {
    val isComplete: Boolean get() = total > 0 && done >= total
}

/** What a finished prepare run produced. */
data class PreparedIconSet(
    val styleId: String,
    val files: List<File>,
) {
    val count: Int get() = files.size
}

/**
 * Renders a whole collection for every app on the device, at full resolution, into one folder.
 *
 * This is the "create my icons" half of applying, and separating it from the hand-off is the point.
 * Before this existed the Samsung screen had a single button that opened Theme Park, which quietly
 * implied DarkFrame had already done something — it had not. Now the two steps are two steps: this
 * one produces a set of finished PNGs the user owns and can see the size of, and the next one hands
 * over to the app that can actually put them on a home screen.
 *
 * ## Cost, and why it is acceptable here
 *
 * Every other path in the engine is capped at preview resolution precisely to keep the device cool.
 * This one is not: an exported icon is destined for a theme engine and has to be the full 512px
 * grid. That is a few hundred large renders, so it is only ever started by an explicit tap, it runs
 * on the engine's two background-priority threads like everything else, it reports progress, and it
 * stops the moment its coroutine is cancelled — which happens when the user leaves the screen.
 * Nothing about it runs on its own.
 */
class IconSetPreparer(
    private val context: Context,
    private val resolver: IconResolver,
) {

    /**
     * Renders [apps] in [style] and writes each one as a PNG.
     *
     * Cooperatively cancellable: the loop checks its own coroutine between icons, so leaving the
     * screen abandons the run at the next icon rather than after all of them.
     *
     * @param onProgress called after each icon, on the calling coroutine's dispatcher.
     */
    @WorkerThread
    suspend fun prepare(
        apps: List<AppIdentity>,
        style: IconStyle,
        onProgress: (PrepareProgress) -> Unit,
    ): PreparedIconSet {
        val directory = directoryFor(style)
        // Cleared first: a previous run of the same collection may have covered apps that are gone,
        // and a folder the user is told holds "their apps" should hold exactly their apps.
        directory.deleteRecursively()
        directory.mkdirs()

        val written = mutableListOf<File>()
        apps.forEachIndexed { index, identity ->
            if (!currentCoroutineContext().isActive) return PreparedIconSet(style.id, written)
            val icon = runCatching {
                resolver.resolve(identity, style, IconRenderPolicy.EXPORT_PX)
            }.getOrNull()
            if (icon != null) {
                writeIcon(directory, identity, icon)?.let { written.add(it) }
            }
            onProgress(PrepareProgress(index + 1, apps.size))
        }
        return PreparedIconSet(style.id, written)
    }

    /** A previously prepared set, or null when this collection has not been prepared yet. */
    fun existing(style: IconStyle): PreparedIconSet? {
        val files = directoryFor(style).listFiles()?.filter { it.isFile }?.sortedBy { it.name }
        return if (files.isNullOrEmpty()) null else PreparedIconSet(style.id, files)
    }

    /**
     * A share intent carrying the whole prepared set.
     *
     * `ACTION_SEND_MULTIPLE` through the existing FileProvider, so the files reach a gallery, a file
     * manager or Theme Park's own picker without DarkFrame asking for a storage permission it would
     * otherwise have no use for. Returns null for an empty set rather than an intent that shares
     * nothing.
     */
    fun shareIntent(set: PreparedIconSet): Intent? {
        if (set.files.isEmpty()) return null
        val uris = set.files.mapNotNull { file ->
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
            }.getOrNull()
        }
        if (uris.isEmpty()) return null
        return Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType("image/png")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** Drops a prepared set. The icons are reproducible, so this is pure cache. */
    @WorkerThread
    fun clear(style: IconStyle) {
        runCatching { directoryFor(style).deleteRecursively() }
    }

    @WorkerThread
    fun clearAll() {
        runCatching { File(context.cacheDir, ROOT).deleteRecursively() }
    }

    /**
     * One folder per collection, under the same exports root the single-icon share uses.
     *
     * Per collection rather than one shared folder so switching collections does not silently leave
     * a mixed set behind, and under `cacheDir` because these are reproducible: the system may
     * reclaim them, and the screen says the set can be prepared again.
     */
    private fun directoryFor(style: IconStyle) = File(File(context.cacheDir, ROOT), style.id)

    private fun writeIcon(directory: File, identity: AppIdentity, icon: Bitmap): File? = runCatching {
        val file = File(directory, fileNameFor(identity))
        file.outputStream().use { stream -> icon.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        file
    }.getOrNull()

    /**
     * Named after the app, not after the component.
     *
     * Someone picking an icon in Theme Park's file browser is looking for "Gmail", so the label
     * leads; the component follows to keep two entry points of one package apart.
     */
    private fun fileNameFor(identity: AppIdentity): String {
        val label = identity.label.replace(UNSAFE, "_").take(MAX_LABEL).trim('_')
        val component = identity.componentKey.replace(UNSAFE, "_")
        return "${label.ifBlank { "app" }}_$component.png"
    }

    private companion object {
        const val ROOT = "prepared"
        const val MAX_LABEL = 40
        val UNSAFE = Regex("[^A-Za-z0-9._-]")
    }
}
