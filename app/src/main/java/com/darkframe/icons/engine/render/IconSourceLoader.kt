package com.darkframe.icons.engine.render

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appcompat.content.res.AppCompatResources
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.ContentBounds
import com.darkframe.icons.engine.domain.IconNormalizer
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.SourceClassifier
import com.darkframe.icons.engine.domain.SourceShape

/** Rasterised source artwork, ready to be seated on a container. */
class SourceArtwork(
    val bitmap: Bitmap,
    val bounds: ContentBounds,
    val shape: SourceShape,
    /** True when [bitmap] is an alpha mask with no useful colour of its own. */
    val isMask: Boolean,
    val fromCurated: Boolean,
)

/**
 * Produces a normalised bitmap for whatever artwork an app happens to ship.
 *
 * The input side of the engine is where Android's variety actually lives, and each case here is a
 * real one seen on real devices:
 *
 *  - **Adaptive icons** are authored on a 108dp canvas of which only the inner 72dp is guaranteed
 *    visible; the rest is bleed for the launcher's mask and parallax. Rendering the whole canvas
 *    makes every adaptive icon ~33% small, so the bleed is cropped off first.
 *  - **Adaptive monochrome layers** (API 33+) exist for exactly the job Noir, Frost, Titanium and
 *    Pure AMOLED want done, and are a far cleaner source than luminance-flattening full-colour
 *    artwork. Used when present, for those collections only.
 *  - **Legacy icons** arrive with their own opaque tile and bezel baked in. Those are detected and
 *    seated as a card rather than pretended to be glyphs (see [SourceClassifier]).
 *  - **Transparent and oddly padded icons** are handled by measuring content rather than canvas.
 *  - **Icons that cannot be loaded at all** return null, and the renderer falls back to a monogram.
 */
class IconSourceLoader(private val context: Context) {

    /**
     * Resolution the source is rasterised at before being scaled into place.
     *
     * Fixed rather than derived from the requested output size, so the same source yields the same
     * measured content bounds no matter which grid density asked for it — a cache served at 192px
     * and a re-render at 96px must not disagree about how large an app's glyph is.
     */
    private val workSize = 288

    @WorkerThread
    fun load(identity: AppIdentity, style: IconStyle, curatedDrawableId: Int?): SourceArtwork? {
        if (curatedDrawableId != null) {
            loadCurated(curatedDrawableId)?.let { return it }
            // Fall through: a curated entry pointing at unusable artwork must not cost the user
            // their icon, it must just lose the override.
        }
        return loadInstalled(identity, style)
    }

    private fun loadCurated(drawableId: Int): SourceArtwork? {
        val drawable = runCatching { AppCompatResources.getDrawable(context, drawableId) }
            .getOrNull() ?: return null
        val bitmap = rasterise(drawable, workSize) ?: return null
        val scan = ContentBoundsScanner.scan(bitmap)
        return SourceArtwork(
            bitmap = bitmap,
            bounds = scan.bounds,
            // Curated artwork is authored as a glyph on transparency by convention, and is
            // trusted as such rather than re-classified.
            shape = SourceShape.GLYPH,
            isMask = false,
            fromCurated = true,
        )
    }

    private fun loadInstalled(identity: AppIdentity, style: IconStyle): SourceArtwork? {
        val drawable = loadActivityIcon(identity) ?: return null

        if (drawable is AdaptiveIconDrawable) {
            monochromeLayerOf(drawable, style)?.let { mask ->
                val bitmap = rasteriseAdaptive(mask) ?: return@let null
                val scan = ContentBoundsScanner.scan(bitmap)
                return SourceArtwork(bitmap, scan.bounds, SourceShape.GLYPH, isMask = true, fromCurated = false)
            }
            val bitmap = rasteriseAdaptive(drawable) ?: return null
            val scan = ContentBoundsScanner.scan(bitmap)
            // An adaptive icon's background layer is opaque by specification, so after cropping to
            // the visible viewport it is full-bleed essentially by definition.
            val shape = SourceClassifier.classify(scan.opaqueFraction, scan.bounds, bitmap.width)
            return SourceArtwork(bitmap, scan.bounds, shape, isMask = false, fromCurated = false)
        }

        val bitmap = rasterise(drawable, workSize) ?: return null
        val scan = ContentBoundsScanner.scan(bitmap)
        val shape = SourceClassifier.classify(scan.opaqueFraction, scan.bounds, bitmap.width)
        return SourceArtwork(bitmap, scan.bounds, shape, isMask = false, fromCurated = false)
    }

    private fun loadActivityIcon(identity: AppIdentity): Drawable? {
        val component = ComponentName(identity.packageName, identity.activityName)
        // The activity icon is preferred over the application icon because a package's launcher
        // activities can declare different icons, and the launcher shows the activity's.
        return runCatching { context.packageManager.getActivityIcon(component) }
            .recoverCatching { context.packageManager.getApplicationIcon(identity.packageName) }
            .getOrNull()
            // Mutated before the engine touches its bounds: PackageManager hands out drawables
            // backed by shared constant state, and resizing one in place corrupts it for every
            // other caller in the process.
            ?.let { drawable -> runCatching { drawable.mutate() }.getOrDefault(drawable) }
    }

    /** The adaptive monochrome layer, when one exists and the style wants it. */
    private fun monochromeLayerOf(drawable: AdaptiveIconDrawable, style: IconStyle): Drawable? {
        if (!style.preferMonochromeLayer) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return monochromeLayer(drawable)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun monochromeLayer(drawable: AdaptiveIconDrawable): Drawable? =
        runCatching { drawable.monochrome }.getOrNull()
            ?.let { layer -> runCatching { layer.mutate() }.getOrDefault(layer) }

    /**
     * Rasterises an adaptive layer and crops the guaranteed-invisible bleed, so the result is the
     * icon as a launcher would actually show it.
     */
    private fun rasteriseAdaptive(drawable: Drawable): Bitmap? {
        val full = rasterise(drawable, workSize) ?: return null
        val inset = IconNormalizer.adaptiveCropInset(workSize)
        val cropped = workSize - inset * 2
        if (inset <= 0 || cropped <= 0) return full
        return runCatching {
            Bitmap.createBitmap(full, inset, inset, cropped, cropped).also { result ->
                if (result !== full) full.recycle()
            }
        }.getOrDefault(full)
    }

    private fun rasterise(drawable: Drawable, size: Int): Bitmap? = runCatching {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bitmap
    }.getOrNull()

    /** Exposed so callers can size work buffers consistently with this loader. */
    fun workResolution(): Int = workSize
}
