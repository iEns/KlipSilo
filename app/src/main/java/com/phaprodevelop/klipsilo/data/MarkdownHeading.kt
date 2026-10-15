package com.phaprodevelop.klipsilo.data

/** Preview body is drawn larger than print size so it sits nearer the editor text. */
internal const val MARKDOWN_PREVIEW_FONT_SCALE = 1.55f

/** Tight top margin for the on-screen markdown preview, in PDF points. */
internal const val MARKDOWN_PREVIEW_MARGIN_PT = 18f

internal fun markdownHeadingFactor(
    level: Int,
    compact: Boolean,
): Float {
    if (!compact) {
        return when (level) {
            1 -> 1.8f
            2 -> 1.5f
            3 -> 1.3f
            4 -> 1.15f
            else -> 1f
        }
    }
    return when (level) {
        1 -> 1.28f
        2 -> 1.16f
        3 -> 1.1f
        4 -> 1.05f
        else -> 1f
    }
}
