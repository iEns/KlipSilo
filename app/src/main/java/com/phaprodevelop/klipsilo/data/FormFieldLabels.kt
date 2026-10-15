package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A widget rectangle in PDF user space. Origin is the bottom left of the page. */
internal data class PdfUserRect(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float,
)

/** One extracted text run in PDF user space. [baseline] is the text baseline. */
internal data class PdfTextRun(
    val text: String,
    val left: Float,
    val baseline: Float,
    val width: Float,
    val height: Float,
)

/** Widget rectangle as fractions of the upright (visually rotated) page. */
internal data class VisualFieldRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal sealed interface ResolvedFieldLabel {
    data class Literal(
        val text: String,
    ) : ResolvedFieldLabel

    data class Numbered(
        val kind: FormFieldType,
        val number: Int,
    ) : ResolvedFieldLabel
}

private val genericFieldName =
    Regex(
        """(?ix)^(
            (?:s|f|cb|chk|check|checkbox|field|text|txt|tx|radio|btn|button|untitled|unnamed|af|tf|box)
            [\s._-]*\d+
            |(?:check\s*box|text\s*field|textfield|radio\s*button|radiobutton)[\s._-]*\d*
            |\d+
        )$""",
    )

/**
 * Tooltip (TU) wins. Otherwise a readable partial name is title-cased.
 * Cryptic names such as "s0" use nearby page text, then "Checkbox 1".
 */
internal fun resolveFormFieldLabel(
    partialName: String,
    alternateName: String?,
    type: FormFieldType,
    nearbyText: String?,
    fallbackNumber: Int,
): ResolvedFieldLabel {
    val tooltip = alternateName?.trim().orEmpty()
    if (tooltip.isNotEmpty()) return ResolvedFieldLabel.Literal(tooltip)
    val partial = partialName.trim()
    if (partial.isNotEmpty() && !genericFieldName.matches(partial)) {
        return ResolvedFieldLabel.Literal(prettifyFieldName(partial))
    }
    val nearby = nearbyText?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    if (nearby.isNotEmpty()) return ResolvedFieldLabel.Literal(nearby.take(80))
    return ResolvedFieldLabel.Numbered(type, fallbackNumber)
}

internal fun prettifyFieldName(raw: String): String {
    val spaced =
        raw
            .replace(Regex("[_./]+"), " ")
            .replace(Regex("(?<=[a-z0-9])([A-Z])")) { match ->
                " ${match.groupValues[1].lowercase(Locale.ROOT)}"
            }
            .replace(Regex("\\s+"), " ")
            .trim()
    if (spaced.isEmpty()) return raw.trim()
    return spaced.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
    }
}

/**
 * Label printed beside or above a widget. Prefers text on the left, then text just above.
 */
internal fun nearbyFieldText(
    widget: PdfUserRect,
    runs: List<PdfTextRun>,
): String? {
    val lines = groupRuns(runs)
    val widgetHeight = (widget.top - widget.bottom).coerceAtLeast(1f)
    val left =
        lines
            .filter { line ->
                line.text.length <= 80 &&
                    line.right <= widget.left + 8f &&
                    widget.left - line.right < 140f &&
                    verticalOverlap(line, widget) >= widgetHeight * 0.3f
            }.minByOrNull { widget.left - it.right }
    if (left != null) return left.text
    return lines
        .filter { line ->
            line.text.length <= 80 &&
                line.bottom >= widget.top - 4f &&
                line.bottom - widget.top < 36f &&
                horizontalOverlap(line.left, line.right, widget.left, widget.right) > 0f
        }.minByOrNull { it.bottom - widget.top }
        ?.text
}

/**
 * Maps a widget to fractions of the page bitmap. [rotation] is the page's clockwise /Rotate.
 */
internal fun widgetVisualFractions(
    widget: PdfUserRect,
    pageLeft: Float,
    pageBottom: Float,
    pageWidth: Float,
    pageHeight: Float,
    rotation: Int,
): VisualFieldRect? {
    if (pageWidth <= 0f || pageHeight <= 0f) return null
    val rot = ((rotation % 360) + 360) % 360
    val visualWidth = if (rot == 90 || rot == 270) pageHeight else pageWidth
    val visualHeight = if (rot == 90 || rot == 270) pageWidth else pageHeight
    if (visualWidth <= 0f || visualHeight <= 0f) return null
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    val xs = floatArrayOf(widget.left, widget.right)
    val ys = floatArrayOf(widget.bottom, widget.top)
    for (x in xs) {
        for (y in ys) {
            val relativeX = x - pageLeft
            val relativeY = y - pageBottom
            val (visualX, visualY) =
                when (rot) {
                    90 -> relativeY to relativeX
                    180 -> (pageWidth - relativeX) to relativeY
                    270 -> (pageHeight - relativeY) to (pageWidth - relativeX)
                    else -> relativeX to (pageHeight - relativeY)
                }
            minX = min(minX, visualX)
            maxX = max(maxX, visualX)
            minY = min(minY, visualY)
            maxY = max(maxY, visualY)
        }
    }
    return VisualFieldRect(
        left = (minX / visualWidth).coerceIn(0f, 1f),
        top = (minY / visualHeight).coerceIn(0f, 1f),
        right = (maxX / visualWidth).coerceIn(0f, 1f),
        bottom = (maxY / visualHeight).coerceIn(0f, 1f),
    )
}

private data class TextLine(
    val text: String,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val baseline: Float,
    val height: Float,
)

private fun groupRuns(runs: List<PdfTextRun>): List<TextLine> {
    val lines = mutableListOf<TextLine>()
    val sorted = runs.sortedWith(compareByDescending<PdfTextRun> { it.baseline }.thenBy { it.left })
    for (run in sorted) {
        val text = run.text.replace(Regex("\\s+"), " ").trim()
        if (text.length < 2 || run.width <= 0f) continue
        val height = run.height.coerceAtLeast(1f)
        val top = run.baseline + height * 0.25f
        val bottom = run.baseline - height * 0.85f
        val right = run.left + run.width
        val previous = lines.lastOrNull()
        val sameLine =
            previous != null &&
                abs(previous.baseline - run.baseline) <= max(2f, height * 0.45f) &&
                run.left - previous.right < height * 1.5f
        if (previous != null && sameLine) {
            lines[lines.lastIndex] =
                previous.copy(
                    text = "${previous.text} $text".trim(),
                    right = max(previous.right, right),
                    top = max(previous.top, top),
                    bottom = min(previous.bottom, bottom),
                )
        } else {
            lines.add(TextLine(text, run.left, right, top, bottom, run.baseline, height))
        }
    }
    return lines
}

private fun verticalOverlap(
    line: TextLine,
    widget: PdfUserRect,
): Float {
    val top = min(line.top, widget.top)
    val bottom = max(line.bottom, widget.bottom)
    return (top - bottom).coerceAtLeast(0f)
}

private fun horizontalOverlap(
    left: Float,
    right: Float,
    otherLeft: Float,
    otherRight: Float,
): Float {
    return (min(right, otherRight) - max(left, otherLeft)).coerceAtLeast(0f)
}
