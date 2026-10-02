package com.darkframe.icons.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.engine.data.StylePreferenceStore
import com.darkframe.icons.engine.domain.IconStyle
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.browser.IconBrowserActivity

/**
 * Collection picker.
 *
 * Reads [IconStyleCatalog] rather than a parallel list of its own, so a collection added to the
 * engine appears here with no change to this screen. Selecting one persists the choice and opens the
 * browser already showing it.
 */
class StylesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = StylePreferenceStore(this)
        val selected = preferences.selectedStyle()
        val entitlement = ProEntitlementStore(this).current()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 56, 44, 48)
            setBackgroundColor(getColor(R.color.df_background))
        }
        root.addView(heading(getString(R.string.styles_title)))
        root.addView(body(getString(R.string.styles_subtitle)))

        IconStyleCatalog.all.forEach { style ->
            root.addView(card(style, isSelected = style.id == selected.id) {
                if (style.tier == ContentTier.PRO && entitlement != Entitlement.PRO) {
                    startActivity(Intent(this, ProActivity::class.java))
                } else {
                    preferences.setSelectedStyle(style)
                    startActivity(Intent(this, IconBrowserActivity::class.java))
                }
            })
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 30f
        setTextColor(getColor(R.color.df_text_primary))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(getColor(R.color.df_text_secondary))
        setPadding(0, 10, 0, 24)
    }

    private fun card(style: IconStyle, isSelected: Boolean, onClick: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            setBackgroundColor(
                getColor(if (isSelected) R.color.df_surface_raised else R.color.df_surface)
            )
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 20 }
            layoutParams = params

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        TextView(context).apply {
                            text = style.displayName
                            textSize = 20f
                            setTextColor(getColor(R.color.df_text_primary))
                        },
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                    )
                    addView(
                        TextView(context).apply {
                            text = if (style.tier == ContentTier.PRO) {
                                getString(R.string.browser_pro_suffix)
                            } else {
                                getString(R.string.styles_free_suffix)
                            }
                            textSize = 11f
                            setTextColor(
                                getColor(
                                    if (style.tier == ContentTier.PRO) {
                                        R.color.df_pro
                                    } else {
                                        R.color.df_text_tertiary
                                    }
                                )
                            )
                        },
                    )
                },
            )
            addView(
                TextView(context).apply {
                    text = style.description
                    textSize = 13f
                    setTextColor(getColor(R.color.df_text_secondary))
                    setPadding(0, 8, 0, 0)
                },
            )
            addView(
                TextView(context).apply {
                    text = getString(
                        if (isSelected) R.string.styles_selected else R.string.styles_select,
                    )
                    textSize = 13f
                    setTextColor(getColor(R.color.df_accent))
                    setPadding(0, 14, 0, 0)
                },
            )
            setOnClickListener { onClick() }
        }
}
