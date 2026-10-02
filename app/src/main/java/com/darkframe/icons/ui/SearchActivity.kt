package com.darkframe.icons.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.R
import com.darkframe.icons.data.DarkFrameCatalog

/**
 * Search over DarkFrame's own handmade artwork.
 *
 * Distinct from the icon browser, which searches every app on the device. This screen exists so the
 * curated set is inspectable, and it says plainly that absence from it does not mean an app is
 * unsupported — otherwise a short list here reads as a short list of supported apps.
 */
class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 56, 44, 40)
            setBackgroundColor(getColor(R.color.df_background))
        }
        root.addView(
            TextView(this).apply {
                text = getString(R.string.search_curated_title)
                textSize = 30f
                setTextColor(getColor(R.color.df_text_primary))
            },
        )
        root.addView(
            TextView(this).apply {
                text = getString(R.string.search_curated_note)
                textSize = 13f
                setTextColor(getColor(R.color.df_text_secondary))
                setPadding(0, 10, 0, 22)
            },
        )

        val input = EditText(this).apply {
            hint = getString(R.string.search_curated_hint)
            setTextColor(getColor(R.color.df_text_primary))
            setHintTextColor(getColor(R.color.df_text_tertiary))
        }
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun render(query: String) {
            results.removeAllViews()
            DarkFrameCatalog.search(query).forEach { item ->
                results.addView(
                    TextView(this).apply {
                        text = item.label
                        textSize = 17f
                        setPadding(8, 20, 8, 20)
                        setTextColor(getColor(R.color.df_text_primary))
                    },
                )
            }
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        root.addView(input)
        root.addView(ScrollView(this).apply { addView(results) })
        setContentView(root)
        render("")
    }
}
