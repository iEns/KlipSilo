// Pure helpers for editing a markup text box (#55): mapping a tap to a caret
// offset inside wrapped text, mapping points between screen and page-layer
// coordinates through the zoom/pan transform, and the enlarged edit-mode
// touch target around the active box.
package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * Character offset in [lines] for a tap at ([tapX], [tapY]) relative to the
 *
 * A tap past the right edge of a line lands at the end of that line; a tap
 * below the last line lands at the end of the text; a tap in the top or left
 * padding lands at the nearest offset (start of the first line).
 */
internal fun caretOffsetForTap(
    tapX: Float,
    tapY: Float,
    lines: List<String>,
    lineHeight: Float,
    measure: (String) -> Float,
): Int {
    if (lines.isEmpty()) return 0
    val lineIndex =
        if (lineHeight <= 0f) 0 else (tapY / lineHeight).toInt().coerceIn(0, lines.lastIndex)
    val start = lines.take(lineIndex).sumOf { it.length + 1 }
    val line = lines[lineIndex]
    if (line.isEmpty() || tapX >= measure(line)) return start + line.length
    var previousWidth = 0f
    for (index in 1..line.length) {
        val width = measure(line.substring(0, index))
        if (tapX < (previousWidth + width) / 2f) return start + index - 1
        previousWidth = width
    }
    return start + line.length
}

/** Screen position of a point given in page-layer coordinates (zoom/pan applied). */
internal fun pageToScreen(
    point: Offset,
    container: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Offset {
    val s = if (scale > 0f) scale else 1f
    val sw = container.width.toFloat()
    val sh = container.height.toFloat()
    return Offset(
        (point.x - sw / 2f) * s + sw / 2f + offsetX,
        (point.y - sh / 2f) * s + sh / 2f + offsetY,
    )
}

/** Page-layer position of a screen point, undoing the zoom/pan transform. */
internal fun screenToPage(
    point: Offset,
    container: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Offset {
    val s = if (scale > 0f) scale else 1f
    val sw = container.width.toFloat()
    val sh = container.height.toFloat()
    return Offset(
        (point.x - offsetX - sw / 2f) / s + sw / 2f,
        (point.y - offsetY - sh / 2f) / s + sh / 2f,
    )
}

/**
 * The edit-mode touch target for the active text box (#55): [box] expanded by
 * [padPx] on all sides plus [trailingPadPx] extra on the trailing (end-of-text)
 * side, so a tap near the last character reaches the caret instead of starting
 * a new box. Only the touch target grows, never the saved box.
 */
internal fun expandedEditTapBounds(
    box: Rect,
    padPx: Float,
    trailingPadPx: Float,
    rtl: Boolean,
): Rect =
    if (rtl) {
        Rect(box.left - trailingPadPx, box.top - padPx, box.right + padPx, box.bottom + padPx)
    } else {
        Rect(box.left - padPx, box.top - padPx, box.right + trailingPadPx, box.bottom + padPx)
    }
