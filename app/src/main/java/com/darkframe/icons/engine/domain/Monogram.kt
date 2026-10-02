package com.darkframe.icons.engine.domain

/**
 * Last-resort glyph.
 *
 * Reached only when a package's own icon cannot be loaded at all — a corrupt install, a package
 * that disappeared mid-render, a resource the system refuses to hand over. Rendering a styled
 * monogram is better than a blank tile and better than crashing the grid.
 */
object Monogram {

    /** One or two initials drawn from a label. Never empty. */
    fun initials(label: String): String {
        val words = label.trim()
            .split(' ', '-', '_', '.', ' ')
            .filter { it.isNotBlank() && it.first().isLetterOrDigit() }

        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(if (words[0].length >= 2) 2 else 1).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }
}
