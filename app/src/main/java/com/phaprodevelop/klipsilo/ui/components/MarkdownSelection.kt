package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.ui.text.TextRange

/**
 * Drops trailing whitespace from [selection] so the select action does not
 * highlight the space after a word (#176). Leading whitespace and spaces
 * inside the selection are kept.
 */
internal fun trimSelectionTrailingWhitespace(
    text: String,
    selection: TextRange,
): TextRange {
    val start = selection.min.coerceIn(0, text.length)
    var end = selection.max.coerceIn(start, text.length)
    while (end > start && text[end - 1].isWhitespace()) end--
    return TextRange(start, end)
}

/**
 * The non-whitespace core of [selection] inside [text], or null when the
 * selection is empty or blank. Markdown markers wrap this range, so whitespace
 * around a word stays outside the markers (#176); spaces inside a multi-word
 * selection are kept.
 */
internal fun markdownContentRange(
    text: String,
    selection: TextRange,
): TextRange? {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    var contentStart = start
    var contentEnd = end
    while (contentStart < contentEnd && text[contentStart].isWhitespace()) contentStart++
    while (contentEnd > contentStart && text[contentEnd - 1].isWhitespace()) contentEnd--
    return if (contentStart == contentEnd) null else TextRange(contentStart, contentEnd)
}
