package com.darkframe.icons.ui.home

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
import com.darkframe.icons.engine.domain.CompleteLook
import com.darkframe.icons.model.ContentTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Renders one look's preview. Implemented by the home screen against the engine's preview cache. */
interface LookPreviewLoader {
    fun peek(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap?
    suspend fun load(look: CompleteLook, widthPx: Int, heightPx: Int): Bitmap
}

/**
 * The horizontal row of collections on the home screen.
 *
 * Each card shows a real render of that look, not a swatch — the point of the row is that a user can
 * see the difference between Noir and Titanium without opening either. Previews are cached, so this
 * row costs one render per look for the lifetime of the process.
 */
class CollectionCardAdapter(
    private val loader: LookPreviewLoader,
    private val scope: CoroutineScope,
    private val previewWidthPx: Int,
    private val previewHeightPx: Int,
    /**
     * Card width in pixels, or null to keep the layout's own.
     *
     * The home row wants a fixed card width so the next card peeks in and invites a swipe; the
     * Collections grid wants the card to fill its column. One card layout serves both rather than a
     * second near-identical one, because two layouts is how a design system starts drifting.
     */
    private val cardWidthPx: Int? = null,
    private val onClick: (CompleteLook) -> Unit,
) : ListAdapter<CompleteLook, CollectionCardAdapter.CardHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card, parent, false)
        return CardHolder(view)
    }

    override fun onBindViewHolder(holder: CardHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: CardHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    inner class CardHolder(view: View) : RecyclerView.ViewHolder(view) {
        // In code rather than in the layout: android:clipToOutline is an API 31 attribute, while
        // the setter has existed since API 21 and DarkFrame's minSdk is 26.
        private val preview: ImageView =
            view.findViewById<ImageView>(R.id.collection_preview).apply { clipToOutline = true }
        private val name: TextView = view.findViewById(R.id.collection_name)
        private val tier: TextView = view.findViewById(R.id.collection_tier)

        private var job: Job? = null
        private var boundId: String? = null

        fun bind(look: CompleteLook) {
            cancel()
            boundId = look.id
            name.text = look.name
            tier.visibility = if (look.tier == ContentTier.PRO) View.VISIBLE else View.GONE
            preview.contentDescription =
                preview.context.getString(R.string.look_preview_description, look.name)
            preview.layoutParams = preview.layoutParams.apply { height = previewHeightPx }
            cardWidthPx?.let { width ->
                itemView.layoutParams = itemView.layoutParams.apply { this.width = width }
            }
            itemView.setOnClickListener { onClick(look) }

            val warm = loader.peek(look, previewWidthPx, previewHeightPx)
            if (warm != null) {
                preview.setImageBitmap(warm)
                return
            }
            preview.setImageDrawable(null)
            job = scope.launch {
                val bitmap = loader.load(look, previewWidthPx, previewHeightPx)
                if (boundId == look.id) preview.setImageBitmap(bitmap)
            }
        }

        fun cancel() {
            job?.cancel()
            job = null
            boundId = null
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CompleteLook>() {
            override fun areItemsTheSame(oldItem: CompleteLook, newItem: CompleteLook) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CompleteLook, newItem: CompleteLook) =
                oldItem == newItem
        }
    }
}
