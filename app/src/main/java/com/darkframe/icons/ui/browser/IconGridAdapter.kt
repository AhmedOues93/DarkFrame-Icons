package com.darkframe.icons.ui.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.AppIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Grid of themed app icons.
 *
 * The adapter renders nothing itself; it asks [ThemedIconLoader] for a finished bitmap. What it does
 * own is the scroll-performance problem that comes with resolving several hundred icons:
 *
 *  - a warm icon is taken straight from memory during bind, so an already-rendered grid scrolls with
 *    no placeholder flicker at all;
 *  - a cold icon is rendered in a per-holder coroutine that is cancelled when the holder is rebound
 *    or recycled, so flinging past a hundred apps does not queue a hundred renders nobody will see;
 *  - the holder records which identity its in-flight job belongs to, so a bitmap that arrives after
 *    its row has been reused is discarded rather than drawn onto the wrong app.
 */
class IconGridAdapter(
    private val loader: ThemedIconLoader,
    private val scope: CoroutineScope,
    private val iconSizePx: Int,
    private val onClick: (AppIdentity) -> Unit,
) : ListAdapter<AppIdentity, IconGridAdapter.IconViewHolder>(DIFF) {

    /**
     * Incremented whenever the selected collection changes. A holder compares it against the
     * generation its bitmap was produced under, so icons from the previous collection cannot win a
     * race against the new one.
     */
    private var styleGeneration = 0

    fun onStyleChanged() {
        styleGeneration++
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_themed_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: IconViewHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    inner class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.icon_image)
        private val label: TextView = view.findViewById(R.id.icon_label)
        private val curated: TextView = view.findViewById(R.id.icon_curated)

        private var job: Job? = null
        private var boundKey: String? = null

        fun bind(identity: AppIdentity) {
            cancel()
            boundKey = identity.componentKey
            label.text = identity.displayLabel()
            curated.visibility = if (loader.isCurated(identity)) View.VISIBLE else View.GONE
            image.contentDescription = image.context.getString(
                R.string.browser_icon_description,
                identity.displayLabel(),
            )
            itemView.setOnClickListener { onClick(identity) }

            val warm = loader.peek(identity, iconSizePx)
            if (warm != null) {
                image.setImageBitmap(warm)
                return
            }

            // Cleared rather than left showing the previous row's icon: a stale image is worse than
            // an empty cell for the few frames a render takes.
            image.setImageDrawable(null)
            val generation = styleGeneration
            job = scope.launch {
                val bitmap = loader.load(identity, iconSizePx)
                if (boundKey == identity.componentKey && generation == styleGeneration) {
                    image.setImageBitmap(bitmap)
                }
            }
        }

        fun cancel() {
            job?.cancel()
            job = null
            boundKey = null
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppIdentity>() {
            override fun areItemsTheSame(oldItem: AppIdentity, newItem: AppIdentity): Boolean =
                oldItem.componentKey == newItem.componentKey &&
                    oldItem.isWorkProfile == newItem.isWorkProfile

            // versionStamp participates: an app that updated while the browser was open is a
            // genuinely different render and must be rebound.
            override fun areContentsTheSame(oldItem: AppIdentity, newItem: AppIdentity): Boolean =
                oldItem == newItem
        }
    }
}
