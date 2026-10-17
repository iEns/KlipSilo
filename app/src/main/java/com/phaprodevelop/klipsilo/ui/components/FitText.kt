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
 * after whitespace (or a dash-level separator we accept as word boundary).
 */
internal fun isMidWordBreak(
    text: String,
    lineStart: Int,
): Boolean =
    lineStart > 0 &&
        lineStart < text.length &&
        !text[lineStart - 1].isWhitespace() &&
        !text[lineStart].isWhitespace()

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
 * Largest font size (stepping down from [base] to the [LABEL_FONT_FLOOR_FRACTION]
 * floor) at which [text] fits on a single line within [maxWidthPx]. Returns
 * null when even the floor size is too wide — the caller should then allow
 * more lines.
 */
internal fun fittedSingleLineSize(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    base: Float,
    maxWidthPx: Int,
): Float? {
    if (maxWidthPx <= 0) return null
    return fittedFontSize(base) { size ->
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
 * individually. Returns null when even the floor size does not fit the
 * longest label — the caller should then allow two lines.
 */
@Composable
fun rememberCommonRowLabelSize(
    labels: List<String>,
    slotWidthPx: Int,
    style: TextStyle,
): Float? {
    val base = style.fontSize
    if (!base.isSp || base.value <= 0f) return null
    val measurer = rememberTextMeasurer()
    val fontScale = LocalDensity.current.fontScale
    return remember(labels, slotWidthPx, style, fontScale) {
        commonFittedSize(
            labels.map { label ->
                fittedSingleLineSize(measurer, label, style, base.value, slotWidthPx)
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
 * .TextMeasurer]. Past the floor it falls back to [maxLines] with
 * [TextOverflow.Ellipsis].
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
            remember(text, style, maxLines, constraints.maxWidth, density.fontScale, floorFraction) {
                if (!bounded) {
                    base to false
                } else {
                    fit(measurer, text, style, base, constraints, maxLines, floorFraction)
                }
            }
        Text(
            text = text,
            color = color,
            style = scaledLabelStyle(style, fitted.first, base),
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
    floorFraction: Float,
): Pair<Float, Boolean> {
    val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)

    fun fits(size: Float): Boolean {
        val result =
            measurer.measure(
                text = text,
                style = scaledLabelStyle(style, size, base),
                softWrap = true,
                maxLines = maxLines,
                constraints = measureConstraints,
            )
        if (result.didOverflowWidth || result.didOverflowHeight) return false
        if (result.lineCount > maxLines) return false
        for (line in 1 until result.lineCount) {
            if (isMidWordBreak(text, result.getLineStart(line))) return false
        }
        return true
    }
    val size = fittedFontSize(base, floorFraction) { fits(it) }
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
