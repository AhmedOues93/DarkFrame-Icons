package com.darkframe.icons.engine.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.WorkerThread
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.engine.domain.Monogram
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperRenderer
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws a miniature home screen for a [CompleteLook].
 *
 * This is what lets a user see what their phone will look like *before* applying anything, which is
 * the point of a Complete Look. It is a real render, not an illustration: the wallpaper comes from
 * the same [WallpaperRenderer] that will set it, and the icons come from the same [IconRenderer]
 * that themes the user's apps, in the same collection.
 *
 * It uses DarkFrame's own curated glyphs rather than the user's installed apps, for two reasons: a
 * preview must look identical on every device so the collections can be compared, and it must cost
 * no package queries — the home screen renders six of these.
 */
class LookPreviewRenderer(context: Context) {

    private val resources = context.resources
    private val packageName = context.packageName
    private val sourceLoader = IconSourceLoader(context)
    private val iconRenderer = IconRenderer()
    private val wallpaperRenderer = WallpaperRenderer()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    /** Glyphs shown in a preview, in grid order. Recognisable shapes that differ from each other. */
    private val previewGlyphs = listOf(
        "dfg_phone", "dfg_messages", "dfg_camera", "dfg_chrome",
        "dfg_spotify", "dfg_maps", "dfg_youtube", "dfg_gmail",
    )

    @WorkerThread
    fun render(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap {
        require(widthPx > 0 && heightPx > 0) { "preview size must be positive" }
        val spec = WallpaperCatalog.byId(look.wallpaperId) ?: WallpaperCatalog.default
        val bitmap = wallpaperRenderer.render(spec, widthPx, heightPx)
        val canvas = Canvas(bitmap)

        val style = look.style
        val margin = widthPx * 0.085f
        val columns = 4
        val gap = widthPx * 0.045f
        val cell = ((widthPx - margin * 2) - gap * (columns - 1)) / columns
        val iconPx = cell.roundToInt().coerceAtLeast(12)

        // A clock plate, standing in for the look's widgets. Drawn from the collection's own
        // container and tint so it belongs to the look rather than being generic chrome.
        drawWidgetPlate(canvas, style.containerColor, style.glyphTint, margin, widthPx, heightPx)

        var drawn = 0
        val top = heightPx * 0.42f
        for (row in 0 until 2) {
            for (column in 0 until columns) {
                val name = previewGlyphs.getOrNull(drawn) ?: break
                val left = margin + column * (cell + gap)
                val y = top + row * (cell + gap * 1.5f)
                if (y + cell > heightPx) break
                drawGlyph(canvas, name, style, iconPx, left, y)
                drawn++
            }
        }
        return bitmap
    }

    private fun drawGlyph(
        canvas: Canvas,
        drawableName: String,
        style: com.darkframe.icons.engine.domain.IconStyle,
        iconPx: Int,
        left: Float,
        top: Float,
    ) {
        @Suppress("DiscouragedApi")
        val id = resources.getIdentifier(drawableName, "drawable", packageName)
        val source = if (id != 0) sourceLoader.loadCuratedArtwork(id, iconPx) else null
        val icon = iconRenderer.render(
            style = style,
            source = source,
            sizePx = iconPx,
            monogramText = Monogram.initials(drawableName.removePrefix("dfg_")),
        )
        canvas.drawBitmap(icon, left, top, paint)
        source?.bitmap?.let { if (!it.isRecycled) it.recycle() }
        icon.recycle()
    }

    /** The widget area of the preview: a time plate in the look's own colours. */
    private fun drawWidgetPlate(
        canvas: Canvas,
        containerColor: Long,
        inkColor: Long,
        margin: Float,
        widthPx: Int,
        heightPx: Int,
    ) {
        val plate = RectF(margin, heightPx * 0.11f, widthPx - margin, heightPx * 0.32f)
        paint.reset()
        paint.isAntiAlias = true
        paint.color = (containerColor or 0xCC000000L).toInt()
        val radius = min(plate.width(), plate.height()) * 0.18f
        canvas.drawRoundRect(plate, radius, radius, paint)

        textPaint.color = inkColor.toInt()
        textPaint.textSize = plate.height() * 0.46f
        textPaint.textAlign = Paint.Align.LEFT
        val bounds = Rect()
        val time = "9:41"
        textPaint.getTextBounds(time, 0, time.length, bounds)
        canvas.drawText(
            time,
            plate.left + plate.width() * 0.07f,
            plate.centerY() + bounds.height() / 2f,
            textPaint,
        )
    }
}
