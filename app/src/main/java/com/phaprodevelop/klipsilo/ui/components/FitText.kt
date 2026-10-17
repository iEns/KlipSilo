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
 * Floor for the label of the SELECTED bottom-navigation item (#62): a
 * selected item always shows its label ("Einstellungen" showed as
 * "Einstellun…"), so it may shrink a little further than
 * [NAV_LABEL_FONT_FLOOR_FRACTION] before an ellipsis.
 */
internal const val NAV_SELECTED_LABEL_MIN_SCALE = 0.45f

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
 * word (#45). A wrapped line must start after whitespace or a separator:
 * hyphen ("OCR-Seite" may wrap after "OCR-"), underscore, '.', '/' or the
 * zero-width space inserted for display (#54). See [isFitBreakChar].
 */
internal fun isMidWordBreak(
    text: String,
    lineStart: Int,
): Boolean =
    lineStart > 0 &&
        lineStart < text.length &&
        !isFitBreakChar(text[lineStart - 1]) &&
        !isFitBreakChar(text[lineStart])

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
 * One decision per label, all at the same scale (#50). Measured before the
 * labels are composed so a long word cannot pick a smaller size than its
 * siblings.
 */
@Composable
internal fun rememberUniformLabelDecisions(
    labels: List<String>,
    slotWidthPx: Int,
    style: TextStyle,
    policy: FitTextPolicy,
): List<FitTextDecision> {
    val base = style.fontSize
    if (!base.isSp || base.value <= 0f || slotWidthPx <= 0) {
        return labels.map { FitTextDecision(1f, it, 1, ellipsized = false) }
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, slotWidthPx, style, density.fontScale, density.density, policy) {
        resolveUniformFit(labels, slotWidthPx.toFloat(), policy) { sample, scale ->
            textWidthPx(measurer, sample, style, base.value, scale)
        }
    }
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
 * Label used inside buttons, chips and other text controls (#45). Layout
 * follows [FitTextPolicy]: one line at the style size, then a word-boundary
 * wrap up to [maxLines], then a shrink down to [floorFraction], then an
 * ellipsis at the end of the last line. A word is never broken.
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
    // Positive [wrapFallbackLines] is ignored. The fixed 64dp top bar used to
    // pass 1, and the extra line was clipped. Callers that need more lines
    // pass them in [maxLines].
    val lines = (maxLines.coerceAtLeast(1) + wrapFallbackLines.coerceAtMost(0)).coerceAtLeast(1)
    if (!baseSize.isSp || baseSize.value <= 0f) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            maxLines = lines,
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
        if (!bounded) {
            Text(
                text = text,
                color = color,
                style = style,
                maxLines = lines,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
            )
            return@BoxWithConstraints
        }
        val decision =
            remember(
                text,
                style,
                lines,
                constraints.maxWidth,
                density.fontScale,
                density.density,
                floorFraction,
            ) {
                resolveFitText(
                    text = text,
                    maxWidthPx = constraints.maxWidth.toFloat(),
                    policy = FitTextPolicy(maxLines = lines, minScale = floorFraction),
                ) { sample, scale ->
                    textWidthPx(measurer, sample, style, base, scale)
                }
            }
        Text(
            text = decision.text,
            color = color,
            style = scaledLabelStyle(style, base * decision.scale, base),
            // Lines come from the policy, already broken on word boundaries.
            // softWrap would let the platform split a word that does not fit.
            softWrap = false,
            maxLines = decision.maxLines,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
        )
    }
}

/**
 * Dialog title (#62). The title slot of an [androidx.compose.material3.AlertDialog]
 * is a narrow slot where a long word breaks mid-word in a plain Text
 * ("Texte zusammenfüg/en"). The [FitTextPolicy] instead: wrap at spaces,
 * shrink down to [TOP_BAR_TITLE_MIN_SCALE], ellipsis last.
 */
@Composable
fun DialogTitleLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    ButtonLabel(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 2,
        floorFraction = TOP_BAR_TITLE_MIN_SCALE,
    )
}

/** Width of [text] at [scale] times the style size, on one line. */
internal fun textWidthPx(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    baseSp: Float,
    scale: Float,
): Float {
    if (text.isEmpty()) return 0f
    return measurer
        .measure(
            text = text,
            style = scaledLabelStyle(style, baseSp * scale, baseSp),
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        ).size.width
        .toFloat()
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
