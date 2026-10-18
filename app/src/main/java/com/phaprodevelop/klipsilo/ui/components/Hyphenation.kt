package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Languages whose hyphenation patterns Android carries (#62). Outside this
 * set (notably ja/ko/zh) a mid-word break is never accepted without a
 * really drawn hyphen, because there is no pattern-valid point to trust.
 */
internal val HYPHENATION_PATTERN_LANGUAGES = setOf("de", "da", "en", "fr", "es", "it", "pt", "ru", "tr")

/**
 * Hyphenation fragments of one word at its final size, per the
 * [HyphenLinesOf] contract (#62). The word is laid out with
 * `Hyphens.Auto` at [maxWidthPx], so the platform breaks it only at
 * pattern-valid points. A break is accepted when a hyphen is really drawn
 * at the line end — detected by the line's visual width exceeding the
 * plain width of its text — and rejected as a raw break otherwise. When no
 * break can be verified at all (hyphen detection is impossible here, e.g.
 * the environment has no pattern data) the break is accepted only for
 * [HYPHENATION_PATTERN_LANGUAGES] locales; the policy then draws the
 * hyphen itself, so the break must still pass the fragment checks in
 * [hyphenatedFormOf].
 *
 * Returns null when the word fits without breaking, overflows even
 * hyphenated, or has no acceptable break.
 */
internal fun hyphenatedWordLines(
    measurer: TextMeasurer,
    word: String,
    style: TextStyle,
    localeList: LocaleList,
    maxLines: Int,
    maxWidthPx: Float,
): List<String>? {
    if (word.isEmpty() || maxWidthPx <= 0f || maxLines < 2 || style.fontSize.value <= 0f) return null
    val layout =
        measurer.measure(
            text = word,
            style =
                style.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph,
                    localeList = localeList,
                ),
            softWrap = true,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = maxWidthPx.roundToInt().coerceAtLeast(1)),
        )
    if (layout.didOverflowWidth || layout.lineCount < 2) return null
    val fragments = ArrayList<String>(layout.lineCount)
    var verified = false
    var raw = false
    for (line in 0 until layout.lineCount) {
        val last = line == layout.lineCount - 1
        val start = layout.getLineStart(line)
        val end = if (last) word.length else layout.getLineEnd(line)
        val piece = word.substring(start, end)
        if (!last && isHyphenDrawn(layout, line, piece, measurer, style)) {
            verified = true
        } else if (!last) {
            raw = true
        }
        fragments += if (last) piece else "$piece-"
    }
    if (raw) {
        if (verified) return null
        // Hyphen detection found nothing anywhere: accept the break only on
        // the strength of the locale's patterns (#62).
        val locale = if (localeList.isEmpty()) null else localeList[0]
        val language = locale?.language?.lowercase() ?: return null
        return if (language in HYPHENATION_PATTERN_LANGUAGES) fragments else null
    }
    return fragments
}

/**
 * True when [line] of [layout] draws a hyphen after its text: the visual
 * line is wider than the same text measured on one line without hyphenation.
 */
private fun isHyphenDrawn(
    layout: TextLayoutResult,
    line: Int,
    piece: String,
    measurer: TextMeasurer,
    style: TextStyle,
): Boolean {
    if (piece.isEmpty()) return false
    val textWidth =
        measurer
            .measure(text = piece, style = style, softWrap = false, maxLines = 1)
            .size.width
            .toFloat()
    val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
    return lineWidth - textWidth > 0.5f
}

/**
 * The [HyphenLinesOf] backed by the app locale (#62). The locale comes from
 * the configuration — `LocaleHelper` writes the chosen per-app language
 * there — so the platform picks the matching hyphenation patterns. The
 * style must carry an sp font size; the returned function answers null for
 * anything it cannot measure or break.
 */
@Composable
internal fun rememberHyphenLinesOf(
    style: TextStyle,
    maxLines: Int,
): HyphenLinesOf {
    val measurer = rememberTextMeasurer()
    val configuration = LocalConfiguration.current
    val languageTag =
        remember(configuration) {
            val locales = configuration.locales
            (if (!locales.isEmpty) locales[0] else Locale.getDefault()).language
        }
    val localeList = remember(languageTag) { LocaleList(languageTag) }
    return remember(measurer, style, localeList, maxLines) {
        { word, scale, maxWidthPx ->
            val base = style.fontSize.value
            if (!style.fontSize.isSp || base <= 0f) {
                null
            } else {
                hyphenatedWordLines(
                    measurer = measurer,
                    word = word,
                    style = scaledLabelStyle(style, base * scale, base),
                    localeList = localeList,
                    maxLines = maxLines,
                    maxWidthPx = maxWidthPx,
                )
            }
        }
    }
}
