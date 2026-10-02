package com.darkframe.icons.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.model.ContentTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The six collections, each showing *this* app.
 *
 * It is the collection picker and the comparison in one control, which is the point: a swatch strip
 * of abstract colours would make someone switch collections one at a time to find out what their
 * own apps look like. Six is a fixed, tiny list, so this is a plain adapter with no diffing — the
 * only thing that changes is which row is selected.
 */
class CollectionSwatchAdapter(
    private val loader: StyledIconLoader,
    private val scope: CoroutineScope,
    private val sizePx: Int,
    private val onSelect: (IconStyle) -> Unit,
) : RecyclerView.Adapter<CollectionSwatchAdapter.SwatchHolder>() {

    private val styles: List<IconStyle> = IconStyleCatalog.all
    private var identity: AppIdentity? = null
    private var selectedId: String = styles.first().id

    fun bind(identity: AppIdentity, selected: IconStyle) {
        this.identity = identity
        this.selectedId = selected.id
        notifyItemRangeChanged(0, styles.size)
    }

    fun select(style: IconStyle) {
        if (style.id == selectedId) return
        val previous = styles.indexOfFirst { it.id == selectedId }
        selectedId = style.id
        if (previous >= 0) notifyItemChanged(previous)
        notifyItemChanged(styles.indexOfFirst { it.id == selectedId })
    }

    override fun getItemCount(): Int = styles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwatchHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_swatch, parent, false)
        return SwatchHolder(view)
    }

    override fun onBindViewHolder(holder: SwatchHolder, position: Int) =
        holder.bind(styles[position])

    override fun onViewRecycled(holder: SwatchHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    inner class SwatchHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.swatch_image)
        private val name: TextView = view.findViewById(R.id.swatch_name)
        private val pro: TextView = view.findViewById(R.id.swatch_pro)

        private var job: Job? = null
        private var boundKey: String? = null

        fun bind(style: IconStyle) {
            cancel()
            name.text = style.displayName
            pro.visibility = if (style.tier == ContentTier.PRO) View.VISIBLE else View.GONE
            itemView.isSelected = style.id == selectedId
            itemView.setOnClickListener { onSelect(style) }

            val app = identity ?: return
            boundKey = key(app, style)
            image.contentDescription = image.context.getString(
                R.string.detail_swatch_description,
                app.displayLabel(),
                style.displayName,
            )

            val warm = loader.peek(app, style, sizePx)
            if (warm != null) {
                image.setImageBitmap(warm)
                return
            }
            image.setImageDrawable(null)
            job = scope.launch {
                val bitmap = loader.load(app, style, sizePx)
                if (boundKey == key(app, style)) image.setImageBitmap(bitmap)
            }
        }

        fun cancel() {
            job?.cancel()
            job = null
            boundKey = null
        }

        private fun key(app: AppIdentity, style: IconStyle) = "${app.componentKey}#${style.id}"
    }
}
