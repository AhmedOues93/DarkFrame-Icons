package com.darkframe.icons.debug

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconStyleCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug-build visual regression surface for the dynamic engine.
 *
 * Every visible row is a real launcher app from this device and every column is one of the six
 * production render styles. RecyclerView keeps the matrix bounded: off-screen rows cancel their
 * work instead of eagerly rendering the entire app drawer.
 */
class IconQaActivity : AppCompatActivity() {
    private val engine by lazy { DarkFrameEngine.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), 0)
        }
        root.addView(TextView(this).apply {
            text = "ICON QA · REAL INSTALLED APPS · 6 COLLECTIONS"
            textSize = 13f
            setPadding(0, 0, 0, dp(8))
        })
        root.addView(header())
        val list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@IconQaActivity)
            adapter = QaAdapter()
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { engine.installedApps.loadCatalog() }
            (list.adapter as QaAdapter).submit(apps)
        }
    }

    private fun header(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(this@IconQaActivity).apply { text = "App"; textSize = 10f },
            LinearLayout.LayoutParams(dp(86), dp(30)))
        IconStyleCatalog.all.forEach { style ->
            addView(TextView(this@IconQaActivity).apply {
                text = style.displayName.take(4)
                textSize = 9f
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
    }

    private inner class QaAdapter : RecyclerView.Adapter<QaHolder>() {
        private var items = emptyList<AppIdentity>()
        fun submit(value: List<AppIdentity>) { items = value; notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QaHolder(
            LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        )
        override fun onBindViewHolder(holder: QaHolder, position: Int) = holder.bind(items[position])
        override fun onViewRecycled(holder: QaHolder) { holder.cancel(); super.onViewRecycled(holder) }
    }

    private inner class QaHolder(private val row: LinearLayout) : RecyclerView.ViewHolder(row) {
        private val label = TextView(this@IconQaActivity).apply { textSize = 10f; maxLines = 2 }
        private val images = IconStyleCatalog.all.map {
            ImageView(this@IconQaActivity).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
        }
        private var jobs = mutableListOf<Job>()
        init {
            row.minimumHeight = dp(62)
            row.addView(label, LinearLayout.LayoutParams(dp(86), ViewGroup.LayoutParams.WRAP_CONTENT))
            images.forEach { row.addView(it, LinearLayout.LayoutParams(0, dp(58), 1f)) }
        }
        fun bind(app: AppIdentity) {
            cancel()
            label.text = app.displayLabel()
            images.forEach { it.setImageDrawable(null) }
            IconStyleCatalog.all.forEachIndexed { index, style ->
                jobs += lifecycleScope.launch {
                    val bitmap = withContext(engine.renderDispatcher) { engine.resolver.resolve(app, style, 144) }
                    images[index].setImageBitmap(bitmap)
                }
            }
        }
        fun cancel() { jobs.forEach { it.cancel() }; jobs.clear() }
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}
