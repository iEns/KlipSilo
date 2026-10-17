package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Font sizes never drop below this fraction of the label's base size (#45).
 * Past the floor the label falls back to [maxLines] with an ellipsis.
 */
internal const val LABEL_FONT_FLOOR_FRACTION = 0.75f

/**
 * Lower floor for single-line bottom navigation labels (#45): a single word
 * has no good second line, so it may shrink to ~60% of the base size before
 * the item falls back to showing its label only when selected.
 */
internal const val NAV_LABEL_FONT_FLOOR_FRACTION = 0.6f

/**
 * Font scale from which layouts reflow into their compact variant (#54):
 * past this the side-by-side rows no longer leave room for their text.
 */
internal const val LARGE_FONT_SCALE = 1.5f

/**
 * Largest font size (stepping down 1sp at a time from [base], floor at
 * [floorFraction] of it) for which [fits] reports the text renders without
 * overflow. Returns `null` when even the floor size does not fit — the caller
 * should then render the floor size with an ellipsis as the last resort.
 */
internal fun fittedFontSize(
    base: Float,
    floorPercentage: Float = LABEL_FONT_FLOOR_FRACTION,
    fits: (Float) -> Boolean,
): Float? {
    require(base > 0f)
    val floor = base * floorPercentage
    var candidate = base
    while (true) {
        if (fits(candidate)) return candidate
        if (abs(candidate - floor) < 0.001f) return null
        candidate = maxOf(floor, candidate - 1f)
    }
}

/**
 * True when a new line of [text] starts at [lineStart] in the middle of a
 * word, i.e. the previous character and the character after the line break
 * are both part of the same word (#45). A wrapped line must always start
 * after whitespace or a separator that reads as a word boundary: a hyphen
 * ("OCR-Seite" may wrap after "OCR-", #45) or an underscore (#54).
 */
internal fun isMidWordBreak(
    text: String,
    lineStart: Int,
): Boolean =
    lineStart > 0 &&
        lineStart < text.length &&
        !isLabelBreakChar(text[lineStart - 1]) &&
        !isLabelBreakChar(text[lineStart])

private fun isLabelBreakChar(c: Char): Boolean = c.isWhitespace() || c == '-' || c == '_'

/** [style] with the font size set to [size]sp, keeping the line height ratio. */
internal fun scaledLabelStyle(
    style: TextStyle,
    size: Float,
    base: Float,
): TextStyle {
    val lineHeight = style.lineHeight
    val scaledLineHeight =
        if (lineHeight.isSp) {
            (lineHeight.value * size / base).sp
        } else {
            lineHeight
        }
    return style.copy(fontSize = size.sp, lineHeight = scaledLineHeight)
}

/**
 * Largest font size (stepping down from [base] to the [floorFraction] floor)
 * at which [text] fits on a single line within [maxWidthPx]. Returns null
 * when even the floor size is too wide — the caller should then allow more
 * lines.
 */
internal fun fittedSingleLineSize(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    base: Float,
    maxWidthPx: Int,
    floorFraction: Float = LABEL_FONT_FLOOR_FRACTION,
): Float? {
    if (maxWidthPx <= 0) return null
    return fittedFontSize(base, floorFraction) { size ->
        val result =
            measurer.measure(
                text = text,
                style = scaledLabelStyle(style, size, base),
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = maxWidthPx),
            )
        !result.didOverflowWidth
    }
}

/**
 * One shared font size for a row whose labels have individual fitted sizes
 * (#50): the smallest of them, so every label in the row renders at the same
 * size. Null when any label could not fit at the floor — the row then falls
 * back to a common layout (e.g. two lines) instead of mixed sizes.
 */
internal fun commonFittedSize(sizes: List<Float?>): Float? =
    if (sizes.isEmpty() || sizes.any { it == null }) {
        null
    } else {
        sizes.filterNotNull().minOrNull()
    }

/**
 * One common font size for a row of equal-width labels (#50): the size the
 * longest label needs to fit its [slotWidthPx] slot on a single line, so all
 * labels in the row render at the same size instead of shrinking
 * individually. [floorFraction] is how far a label may shrink before the row
 * gives up (single-word tool labels use ~60%, #50). Returns null when even
 * the floor size does not fit the longest label — the caller should then
 * allow two lines (never inside a word).
 */
@Composable
fun rememberCommonRowLabelSize(
    labels: List<String>,
    slotWidthPx: Int,
    style: TextStyle,
    floorFraction: Float = LABEL_FONT_FLOOR_FRACTION,
): Float? {
    val base = style.fontSize
    if (!base.isSp || base.value <= 0f) return null
    val measurer = rememberTextMeasurer()
    val fontScale = LocalDensity.current.fontScale
    return remember(labels, slotWidthPx, style, fontScale, floorFraction) {
        commonFittedSize(
            labels.map { label ->
                fittedSingleLineSize(measurer, label, style, base.value, slotWidthPx, floorFraction)
            },
        )
    }
}

/**
 * Label used inside buttons, chips and other text controls so a long word is
 * never broken mid-word at large font scales (#45). Given the width the
 * composable ends up with, the font size is stepped down (1sp at a time,
 * floor ~75% of the style size) until every word fits on a line and the text
 * fits within [maxLines] — measured with a [androidx.compose.ui.text
 * .TextMeasurer]. When [wrapFallbackLines] > 0, that many extra lines are
 * tried at word boundaries as a last resort before the ellipsis (#47). Past
 * the floor it falls back to a single line with [TextOverflow.Ellipsis] —
 * never a soft wrap inside a word (#45/#50).
 */
@Composable
fun ButtonLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = Color.Unspecified,
    maxLines: Int = 2,
    textAlign: TextAlign? = null,
    floorFraction: Float = LABEL_FONT_FLOOR_FRACTION,
    wrapFallbackLines: Int = 0,
) {
    val baseSize = style.fontSize
    if (!baseSize.isSp || baseSize.value <= 0f) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
        return
    }
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val base = baseSize.value
        val bounded =
            constraints.hasBoundedWidth &&
                constraints.maxWidth > 0 &&
                constraints.maxWidth != Constraints.Infinity
        val fitted =
            remember(
                text,
                style,
                maxLines,
                wrapFallbackLines,
                constraints.maxWidth,
                density.fontScale,
                floorFraction,
            ) {
                if (!bounded) {
                    base to false
                } else {
                    fit(
                        measurer,
                        text,
                        style,
                        base,
                        constraints,
                        maxLines,
                        wrapFallbackLines,
                        floorFraction,
                    )
                }
            }
        Text(
            text = text,
            color = color,
            style = scaledLabelStyle(style, fitted.first, base),
            // The ellipsis fallback must never soft-wrap: a word too wide for
            // the line would be broken mid-word ("Auswa/hl", #45/#50), so the
            // last resort is one clipped line ending in an ellipsis.
            softWrap = !fitted.second,
            maxLines = maxLines,
            overflow = if (fitted.second) TextOverflow.Ellipsis else TextOverflow.Clip,
            textAlign = textAlign,
        )
    }
}

/** Returns the size to render and whether the ellipsis fallback is active. */
private fun fit(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    base: Float,
    constraints: Constraints,
    maxLines: Int,
    wrapFallbackLines: Int,
    floorFraction: Float,
): Pair<Float, Boolean> {
    val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)

    fun fits(
        size: Float,
        lines: Int,
    ): Boolean {
        val result =
            measurer.measure(
                text = text,
                style = scaledLabelStyle(style, size, base),
                softWrap = true,
                maxLines = lines,
                constraints = measureConstraints,
            )
        if (result.didOverflowWidth || result.didOverflowHeight) return false
        if (result.lineCount > lines) return false
        for (line in 1 until result.lineCount) {
            if (isMidWordBreak(text, result.getLineStart(line))) return false
        }
        return true
    }
    val size =
        fittedFontSize(base, floorFraction) { fits(it, maxLines) }
            // Last resort before the ellipsis (#47): extra lines, still only
            // at word boundaries.
            ?: if (wrapFallbackLines > 0) {
                fittedFontSize(base, floorFraction) { fits(it, maxLines + wrapFallbackLines) }
            } else {
                null
            }
    return if (size != null) {
        size to false
    } else {
        (base * floorFraction) to true
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun ButtonLabelPreview() {
    MaterialTheme {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                ButtonLabel("Auswahl speichern")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    ButtonLabel("Text speichern")
                }
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    ButtonLabel("Analysieren")
                }
            }
        }
    }
}
