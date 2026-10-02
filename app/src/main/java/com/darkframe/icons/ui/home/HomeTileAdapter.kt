package com.darkframe.icons.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R

/** One compact entry point on the home screen. */
data class HomeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    /**
     * Marks the Pro tile, which carries the one gold accent on the home screen.
     *
     * A flag rather than a second tile layout: the two differ by a text colour, and a near-duplicate
     * layout is how a design system starts drifting.
     */
    val accent: Boolean = false,
    val onClick: () -> Unit,
)

/** The compact grid under the collections: wallpapers, widgets, apps, favourites and Pro. */
class HomeTileAdapter(private var tiles: List<HomeTile>) :
    RecyclerView.Adapter<HomeTileAdapter.TileHolder>() {

    /**
     * The tile set is fixed; only the subtitles change (a saved count, a wallpaper total). Rebinding
     * the existing range says that precisely, instead of telling RecyclerView that everything it
     * knows is invalid.
     */
    fun submit(next: List<HomeTile>) {
        val previousCount = tiles.size
        tiles = next
        if (previousCount == next.size) {
            notifyItemRangeChanged(0, next.size)
        } else {
            notifyItemRangeRemoved(0, previousCount)
            notifyItemRangeInserted(0, next.size)
        }
    }

    override fun getItemCount(): Int = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_tile, parent, false)
        return TileHolder(view)
    }

    override fun onBindViewHolder(holder: TileHolder, position: Int) = holder.bind(tiles[position])

    class TileHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.tile_title)
        private val subtitle: TextView = view.findViewById(R.id.tile_subtitle)

        fun bind(tile: HomeTile) {
            title.text = tile.title
            subtitle.text = tile.subtitle
            subtitle.setTextColor(
                itemView.context.getColor(
                    if (tile.accent) R.color.df_pro else R.color.df_text_secondary,
                ),
            )
            itemView.setOnClickListener { tile.onClick() }
        }
    }
}
