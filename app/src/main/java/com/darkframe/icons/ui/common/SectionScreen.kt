package com.darkframe.icons.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.darkframe.icons.R

/**
 * The shared scaffold for DarkFrame's secondary screens.
 *
 * Every screen that is a title plus a stack of cards uses this, so spacing, type and card treatment
 * come from one place rather than being re-approximated per screen. The alternative — each activity
 * assembling its own views with inline colours and sizes — is exactly what made the old build look
 * like a prototype.
 */
class SectionScreen(private val activity: DarkFrameActivity) {

    private lateinit var container: LinearLayout

    fun setUp(title: String, subtitle: String? = null): SectionScreen {
        activity.setContentView(R.layout.activity_simple)
        activity.findViewById<NestedScrollView>(R.id.simple_scroll).applySystemBarPadding()
        activity.findViewById<TextView>(R.id.simple_title).text = title
        activity.findViewById<TextView>(R.id.simple_subtitle).apply {
            if (subtitle.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                text = subtitle
                visibility = View.VISIBLE
            }
        }
        container = activity.findViewById(R.id.simple_content)
        return this
    }

    /** A small all-caps heading between groups of rows. */
    fun header(text: String): SectionScreen {
        val view = TextView(activity, null, 0, R.style.DF_Text_Label)
        view.text = text
        add(view)
        return this
    }

    /** A card of prose. Used sparingly: long explanations belong in Settings, not on a flow. */
    fun section(heading: String, body: String): SectionScreen {
        val view = inflate(R.layout.item_section)
        view.findViewById<TextView>(R.id.section_heading).text = heading
        view.findViewById<TextView>(R.id.section_body).text = body
        add(view)
        return this
    }

    /** A tappable row: title, optional subtitle, optional trailing value. */
    fun row(
        title: String,
        subtitle: String? = null,
        value: String? = null,
        onClick: (() -> Unit)? = null,
    ): SectionScreen {
        val view = inflate(R.layout.item_action_row)
        view.findViewById<TextView>(R.id.row_title).text = title
        view.findViewById<TextView>(R.id.row_subtitle).apply {
            if (subtitle.isNullOrBlank()) visibility = View.GONE else {
                text = subtitle
                visibility = View.VISIBLE
            }
        }
        view.findViewById<TextView>(R.id.row_value).apply {
            if (value.isNullOrBlank()) visibility = View.GONE else {
                text = value
                visibility = View.VISIBLE
            }
        }
        // A row without an action keeps its card: it is information inside the same surface, not a
        // different kind of thing. Only the click handling differs.
        if (onClick != null) view.setOnClickListener { onClick() } else view.isClickable = false
        add(view)
        return this
    }

    /**
     * A numbered step, with its own action and a status line that can be updated later.
     *
     * Returns a handle rather than the screen, because a step's state changes while the user is
     * looking at it — a prepare run reports progress — and rebuilding the whole screen per update
     * would throw away scroll position and re-inflate every card.
     */
    fun step(
        number: Int,
        title: String,
        body: String,
        actionLabel: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): StepHandle {
        val view = inflate(R.layout.item_step)
        view.findViewById<TextView>(R.id.step_number).text = number.toString()
        view.findViewById<TextView>(R.id.step_title).text = title
        view.findViewById<TextView>(R.id.step_body).text = body
        val action = view.findViewById<TextView>(R.id.step_action)
        action.text = actionLabel
        action.isEnabled = enabled
        action.setOnClickListener { if (action.isEnabled) onClick() }
        add(view)
        return StepHandle(action, view.findViewById(R.id.step_status))
    }

    /** Live handles into one step's action and status line. */
    class StepHandle(private val action: TextView, private val status: TextView) {
        fun setActionLabel(text: String) {
            action.text = text
        }

        /** Replaces the action, for a step whose behaviour is decided after it is laid out. */
        fun setOnAction(onClick: () -> Unit) {
            action.setOnClickListener { if (action.isEnabled) onClick() }
        }

        fun setActionEnabled(enabled: Boolean) {
            action.isEnabled = enabled
        }

        /** Null or blank hides the line, so a step with nothing to report shows nothing. */
        fun setStatus(text: String?) {
            if (text.isNullOrBlank()) {
                status.visibility = View.GONE
            } else {
                status.text = text
                status.visibility = View.VISIBLE
            }
        }
    }

    /** The screen's primary action. At most one per screen, by design. */
    fun primaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit): SectionScreen {
        add(button(R.style.DF_Button_Primary, text, enabled, onClick))
        return this
    }

    fun secondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit): SectionScreen {
        add(button(R.style.DF_Button_Secondary, text, enabled, onClick))
        return this
    }

    fun caption(text: String): SectionScreen {
        val view = TextView(activity, null, 0, R.style.DF_Text_Caption)
        view.text = text
        add(view)
        return this
    }

    fun custom(view: View): SectionScreen {
        add(view)
        return this
    }

    private fun button(styleRes: Int, text: String, enabled: Boolean, onClick: () -> Unit): View {
        val view = TextView(activity, null, 0, styleRes)
        view.text = text
        view.isEnabled = enabled
        view.setOnClickListener { if (enabled) onClick() }
        // A style cannot supply layout params — those belong to the parent — so the control height
        // from the design system is applied here rather than left to wrap_content.
        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            activity.resources.getDimensionPixelSize(R.dimen.df_control_height),
        )
        return view
    }

    private fun inflate(layout: Int): View =
        LayoutInflater.from(activity).inflate(layout, container, false)

    private fun add(view: View) {
        val params = view.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        params.topMargin = activity.resources.getDimensionPixelSize(R.dimen.df_space_3)
        view.layoutParams = params
        container.addView(view)
    }
}
