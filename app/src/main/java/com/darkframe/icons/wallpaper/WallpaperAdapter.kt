package com.darkframe.icons.wallpaper

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.wallpaper.WallpaperSpec
import com.darkframe.icons.model.ContentTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Produces wallpaper thumbnails. Implemented against the engine's renderer. */
interface WallpaperPreviewLoader {
    fun peek(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap?
    suspend fun load(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap
}

/**
 * The wallpaper grid.
 *
 * Thumbnails are drawn at cell size, never at screen size: a wallpaper is only rendered at the
 * device's real resolution when it is actually being set. Each cell cancels its own render when it
 * is recycled, so scrolling a category does not queue work for cells that have gone.
 */
class WallpaperAdapter(
    private val loader: WallpaperPreviewLoader,
    private val scope: CoroutineScope,
    private val onClick: (WallpaperSpec) -> Unit,
) : ListAdapter<WallpaperSpec, WallpaperAdapter.WallpaperHolder>(DIFF) {

    private var cellWidth = 0
    private var cellHeight = 0

    fun setCellSize(widthPx: Int, heightPx: Int) {
        if (widthPx == cellWidth && heightPx == cellHeight) return
        cellWidth = widthPx
        cellHeight = heightPx
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper, parent, false)
        return WallpaperHolder(view)
    }

    override fun onBindViewHolder(holder: WallpaperHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onViewRecycled(holder: WallpaperHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    inner class WallpaperHolder(view: View) : RecyclerView.ViewHolder(view) {
        // See CollectionCardAdapter: android:clipToOutline is API 31, the setter is API 21.
        private val image: ImageView =
            view.findViewById<ImageView>(R.id.wallpaper_image).apply { clipToOutline = true }
        private val title: TextView = view.findViewById(R.id.wallpaper_title)
        private val tier: TextView = view.findViewById(R.id.wallpaper_tier)

        private var job: Job? = null
        private var boundId: String? = null

        fun bind(spec: WallpaperSpec) {
            cancel()
            boundId = spec.id
            title.text = spec.title
            tier.visibility = if (spec.tier == ContentTier.PRO) View.VISIBLE else View.GONE
            image.contentDescription =
                image.context.getString(R.string.wallpaper_preview_description, spec.title)
            itemView.setOnClickListener { onClick(spec) }

            if (cellWidth <= 0 || cellHeight <= 0) return
            image.layoutParams = image.layoutParams.apply { height = cellHeight }

            val warm = loader.peek(spec, cellWidth, cellHeight)
            if (warm != null) {
                image.setImageBitmap(warm)
                return
            }
            image.setImageDrawable(null)
            job = scope.launch {
                val bitmap = loader.load(spec, cellWidth, cellHeight)
                if (boundId == spec.id) image.setImageBitmap(bitmap)
            }
        }

        fun cancel() {
            job?.cancel()
            job = null
            boundId = null
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WallpaperSpec>() {
            override fun areItemsTheSame(oldItem: WallpaperSpec, newItem: WallpaperSpec) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WallpaperSpec, newItem: WallpaperSpec) =
                oldItem == newItem
        }
    }
}
