package com.darkframe.icons.qa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * One row per app, one cell per collection.
 *
 * Each row costs six renders when cold, which is six times what the browser's grid costs per item,
 * so the scheduling discipline matters more here than anywhere else: cells are bound from the cache
 * when warm, cold cells launch one cancellable job each, and every job is cancelled on rebind and
 * on recycle. Flinging through two hundred apps therefore leaves at most the visible rows' work
 * outstanding rather than queueing twelve hundred renders nobody will see.
 *
 * The cells are built once in the view holder and re-bound, not inflated per bind: a row of six
 * ImageViews inflated on every bind is the kind of thing that makes a debug screen slower than the
 * product it is meant to be inspecting.
 */
class QaMatrixAdapter(
    private val loader: QaIconLoader,
    private val scope: CoroutineScope,
    private var cellSizePx: Int,
) : ListAdapter<AppIdentity, QaMatrixAdapter.RowHolder>(DIFF) {

    private val styles: List<IconStyle> = IconStyleCatalog.all
    private var checker = false

    /**
     * Rebinding every row invalidates the cells, but not the cache: a size the user has already
     * looked at is still in memory, and the bucketing in `IconRenderPolicy` means neighbouring
     * sizes often share one entry.
     */
    fun setCellSize(px: Int) {
        if (px == cellSizePx) return
        cellSizePx = px
        notifyItemRangeChanged(0, itemCount)
    }

    fun setChecker(enabled: Boolean) {
        if (enabled == checker) return
        checker = enabled
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qa_row, parent, false)
        return RowHolder(view)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: RowHolder) {
        holder.cancel()
        super.onViewRecycled(holder)
    }

    inner class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.qa_row_label)
        private val detail: TextView = view.findViewById(R.id.qa_row_detail)
        private val cells: LinearLayout = view.findViewById(R.id.qa_row_cells)

        private val images: List<ImageView> = styles.map { style ->
            ImageView(view.context).also { image ->
                image.scaleType = ImageView.ScaleType.FIT_CENTER
                image.tag = style.id
                cells.addView(image)
            }
        }

        private val jobs = arrayOfNulls<Job>(styles.size)
        private var boundKey: String? = null

        fun bind(identity: AppIdentity) {
            cancel()
            boundKey = identity.componentKey
            label.text = identity.displayLabel()
            detail.text = buildString {
                append(identity.packageName)
                if (loader.isCurated(identity)) {
                    append(" · ")
                    append(itemView.context.getString(R.string.qa_curated))
                }
            }

            styles.forEachIndexed { index, style ->
                val image = images[index]
                val params = image.layoutParams as LinearLayout.LayoutParams
                params.width = cellSizePx
                params.height = cellSizePx
                params.marginEnd = cells.context.resources
                    .getDimensionPixelSize(R.dimen.df_space_1)
                image.layoutParams = params
                image.setBackgroundResource(if (checker) R.drawable.qa_checker else 0)
                image.contentDescription = image.context.getString(
                    R.string.qa_cell_description,
                    identity.displayLabel(),
                    style.displayName,
                )

                val warm = loader.peek(identity, style, cellSizePx)
                if (warm != null) {
                    image.setImageBitmap(warm)
                    return@forEachIndexed
                }
                image.setImageDrawable(null)
                jobs[index] = scope.launch {
                    val bitmap = loader.load(identity, style, cellSizePx)
                    // The holder may have been rebound to a different app while this was rendering;
                    // drawing then would put one app's icon on another's row, which in a QA tool is
                    // worse than a blank cell because it looks like a renderer bug.
                    if (boundKey == identity.componentKey) image.setImageBitmap(bitmap)
                }
            }
        }

        fun cancel() {
            jobs.indices.forEach { index ->
                jobs[index]?.cancel()
                jobs[index] = null
            }
            boundKey = null
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppIdentity>() {
            override fun areItemsTheSame(oldItem: AppIdentity, newItem: AppIdentity): Boolean =
                oldItem.componentKey == newItem.componentKey &&
                    oldItem.isWorkProfile == newItem.isWorkProfile

            override fun areContentsTheSame(oldItem: AppIdentity, newItem: AppIdentity): Boolean =
                oldItem == newItem
        }
    }
}
