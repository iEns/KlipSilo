package com.phaprodevelop.klipsilo.ui.components

import kotlin.math.abs

/**
 * How a label gives up space (#45 #47 #50 #54 #62).
 *
 * 1. Draw at the style size on one line.
 * 2. Wrap at word boundaries only, up to [maxLines].
 * 3. Shrink, in [FIT_SCALE_STEP] steps, down to [minScale]. A uniform group
 *    uses one scale for every sibling.
 * 4. Hyphenate a single word that is still wider than the line at
 *    [minScale] (#62): the platform breaks it at a pattern-valid point and
 *    a visible hyphen ends the line ("Datenschutz-/erklärung"). The
 *    platform lays a word out with one break choice per width; when that
 *    choice ends in a too-short fragment the width is retried narrower so
 *    the platform breaks earlier (#62). See [hyphenatedFormOf] and the
 *    [HyphenLinesOf] contract.
 * 5. Ellipsis at the end of the last line, only as the last resort. A line
 *    never starts mid-word without a hyphen; a single word that cannot fit
 *    or hyphenate is cut with an ellipsis, not broken.
 *
 * [minScale] is a fraction of the style size (1 keeps the style size).
 */
internal data class FitTextPolicy(
    val maxLines: Int = 2,
    val minScale: Float = LABEL_FONT_FLOOR_FRACTION,
)

/**
 * Hyphenation fragments of one long word at [FitTextPolicy.minScale] (#62).
 * Given `(word, scale, maxWidthPx)` the platform hyphenation returns the
 * word split at pattern-valid points, every fragment but the last ending
 * with the visible hyphen ("Ausgabe-", "resolution"), or null when the
 * platform does not break the word into lines that all fit [maxWidthPx].
 * Fragments are only accepted when a hyphen is really drawn at the line
 * end, or — where that cannot be detected — the locale carries hyphenation
 * patterns and every fragment has at least three letters (see
 * [hyphenatedWordLines]).
 */
internal typealias HyphenLinesOf = (word: String, scale: Float, maxWidthPx: Float) -> List<String>?

/** Scale of the style size, the text to draw, and whether it was cut. */
internal data class FitTextDecision(
    val scale: Float,
    val text: String,
    val maxLines: Int,
    val ellipsized: Boolean,
)

/** Horizontal step while shrinking from the style size down to [FitTextPolicy.minScale]. */
internal const val FIT_SCALE_STEP = 0.05f

internal const val FIT_ELLIPSIS = "…"

/**
 * Title shrink floor (#47): two lines at about 60% of titleLarge still fit
 * "Formular ausfüllen" and "Seiten bearbeiten" at font scale 2.0.
 */
internal const val TOP_BAR_TITLE_MIN_SCALE = 0.6f

/**
 * Clip titles stay at the body size (#54). Anything that needs more than
 * [FitTextPolicy.maxLines] is ellipsized instead of shrunk onto one line.
 */
internal const val CLIP_TITLE_MIN_SCALE = 1f

/**
 * Tool-rail floor (#50). Lower than the nav floor so the longest single
 * word ("Auswahl") still fits its slot at font scale 2.0; the whole row
 * uses this one scale.
 */
internal const val TOOL_LABEL_MIN_SCALE = 0.45f

/**
 * '_' '-' '.' '/' whitespace, the zero-width space U+200B and the soft
 * hyphen U+00AD are break opportunities (#45 #54 #62). A soft hyphen is a
 * word boundary for the mid-word check too, and it is preferred over the
 * pattern hyphenation: the word splitting in [hyphenatedFormOf] stops at it,
 * so the platform hyphenation is only asked for the pieces between the soft
 * hyphens.
 */
internal fun isFitBreakChar(c: Char): Boolean =
    c.isWhitespace() || c == '-' || c == '_' || c == '.' || c == '/' ||
        c == '\u200B' || c == '\u00AD'

/**
 * Characters of scripts written without spaces — Japanese, Korean, Chinese
 * and the other CJK scripts (#62). Between two of them a line may start:
 * that is the normal line breaking for these scripts, and it must happen
 * before an ellipsis ("プライバシーポリシ…" was cut instead of wrapped).
 */
internal fun isCjkChar(c: Char): Boolean {
    val code = c.code
    return code in 0x1100..0x11FF || // Hangul jamo
        code in 0x3000..0x30FF || // CJK punctuation, Hiragana, Katakana
        code in 0x3400..0x4DBF || // CJK ideographs, extension A
        code in 0x4E00..0x9FFF || // CJK ideographs
        code in 0xAC00..0xD7A3 || // Hangul syllables
        code in 0xF900..0xFAFF || // CJK compatibility ideographs
        code in 0xFF00..0xFFA0 // fullwidth forms, halfwidth katakana
}

/**
 * CJK characters a wrapped line must not START with (kinsoku shori, #62):
 * closing punctuation and the small kana and the prolonged sound mark that
 * belong to the character before them.
 */
internal fun isCjkNoLineStartChar(c: Char): Boolean =
    c in "、。，．：；！？〜・" ||
        c in "〉》」』）］｝〉》」』）］｝" ||
        c in "ぁぃぅぇぉっゃゅょゎゕゖゝゞ" ||
        c in "ァィゥェォッャュョヮヵヶーヽヾ"

/** Scales from 1 down to [minScale], inclusive, largest first. */
internal fun fitScales(minScale: Float): List<Float> {
    val min = minScale.coerceIn(0.05f, 1f)
    val steps = ((1f - min) / FIT_SCALE_STEP).toInt()
    val scales = ArrayList<Float>(steps + 2)
    for (i in 0..steps) {
        val scale = 1f - i * FIT_SCALE_STEP
        if (scales.isEmpty() || abs(scales.last() - scale) > 0.001f) scales += scale
    }
    if (abs(scales.last() - min) > 0.001f) scales += min
    return scales
}

/**
 * Largest scale in the policy at which [text] fits [maxWidthPx] without an
 * ellipsis, otherwise [FitTextPolicy.minScale] with an end ellipsis. At the
 * floor, a single word that is still wider than the line is hyphenated
 * first when [hyphenLinesOf] can break it (#62).
 */
internal fun resolveFitText(
    text: String,
    maxWidthPx: Float,
    policy: FitTextPolicy,
    widthOf: (String, Float) -> Float,
    hyphenLinesOf: HyphenLinesOf? = null,
): FitTextDecision {
    val maxLines = policy.maxLines.coerceAtLeast(1)
    val minScale = policy.minScale.coerceIn(0.05f, 1f)
    if (text.isEmpty() || maxWidthPx <= 0f) {
        return FitTextDecision(1f, text, 1, ellipsized = false)
    }
    for (scale in fitScales(minScale)) {
        val candidate = resolveAtScale(text, maxWidthPx, scale, maxLines, widthOf)
        if (!candidate.ellipsized) return candidate
        if (abs(scale - minScale) < 0.001f) {
            val hyphenated = hyphenateAtFloor(text, maxWidthPx, scale, maxLines, widthOf, hyphenLinesOf)
            return hyphenated ?: candidate
        }
    }
    return resolveAtScale(text, maxWidthPx, minScale, maxLines, widthOf)
}

/**
 * Step d of the policy (#62): at the floor scale, replace every word that
 * is still wider than the line with its pattern-valid hyphenation
 * fragments, then wrap the rebuilt text at those points. Returns null when
 * nothing needs hyphenating, the platform cannot break a too-wide word, a
 * fragment has fewer than three letters, or the rebuilt text still needs an
 * ellipsis — the caller then keeps its plain ellipsized decision.
 */
private fun hyphenateAtFloor(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    maxLines: Int,
    widthOf: (String, Float) -> Float,
    hyphenLinesOf: HyphenLinesOf?,
): FitTextDecision? {
    if (hyphenLinesOf == null) return null
    val rebuilt = hyphenatedFormOf(text, scale, maxWidthPx, widthOf, hyphenLinesOf) ?: return null
    val decision = resolveAtScale(rebuilt, maxWidthPx, scale, maxLines, widthOf)
    return if (!decision.ellipsized) decision else null
}

/**
 * [text] with every word wider than [maxWidthPx] at [scale] replaced by its
 * hyphenation fragments joined back together ("Ausgabe-/resolution" keeps
 * the hyphen, so the wrap only breaks where a hyphen is drawn). Null when
 * one of those words cannot be hyphenated or a fragment has fewer than
 * three letters on either side of a break (#62).
 *
 * The platform lays a word out with one break choice per width. When that
 * choice ends in a too-short fragment ("Ausgaberesoluti-/on" — German
 * patterns allow the ti/on break and the layout greedily fills the line,
 * #62) the same layout at a narrower width breaks earlier
 * ("Ausgabe-/resolution"), so the widths are retried, narrowest last.
 */
internal fun hyphenatedFormOf(
    text: String,
    scale: Float,
    maxWidthPx: Float,
    widthOf: (String, Float) -> Float,
    hyphenLinesOf: HyphenLinesOf,
): String? {
    val pieces = ArrayList<String>()
    var changed = false
    var index = 0
    while (index < text.length) {
        if (isFitBreakChar(text[index])) {
            pieces += text[index].toString()
            index += 1
            continue
        }
        var end = index
        while (end < text.length && !isFitBreakChar(text[end])) end += 1
        val word = text.substring(index, end)
        if (fitsWidth(widthOf(word, scale), maxWidthPx)) {
            pieces += word
        } else {
            val fragments = hyphenFragments(word, scale, maxWidthPx, hyphenLinesOf) ?: return null
            // The fragments already carry the hyphen; the joins between them
            // are exactly the line breaks the platform chose.
            pieces += fragments.joinToString(separator = "")
            changed = true
        }
        index = end
    }
    return if (changed) pieces.joinToString(separator = "") else null
}

/**
 * [hyphenLinesOf] for one word at [maxWidthPx], then at narrower widths:
 * the platform's one break choice per width can end in a fragment with
 * fewer than three letters ("…resoluti-/on", #62) — a narrower layout
 * breaks earlier ("Ausgabe-/resolution"). Null when no width yields
 * fragments that all pass the three-letter rule.
 */
private fun hyphenFragments(
    word: String,
    scale: Float,
    maxWidthPx: Float,
    hyphenLinesOf: HyphenLinesOf,
): List<String>? =
    (listOf(maxWidthPx) + HYPHEN_RETRY_FRACTIONS.map { maxWidthPx * it })
        .firstNotNullOfOrNull { width ->
            hyphenLinesOf(word, scale, width)?.takeIf(::fragmentsHaveThreeLetters)
        }

private val HYPHEN_RETRY_FRACTIONS = listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)

private fun fragmentsHaveThreeLetters(fragments: List<String>): Boolean =
    fragments.all { fragment -> fragment.count { it.isLetter() } >= 3 }

/**
 * One scale for every text (#50). The largest scale at which all of them fit
 * without an ellipsis, or [FitTextPolicy.minScale] when one of them must be
 * cut. Every decision uses that same scale.
 */
internal fun resolveUniformFit(
    texts: List<String>,
    slotWidthPx: Float,
    policy: FitTextPolicy,
    widthOf: (String, Float) -> Float,
): List<FitTextDecision> {
    if (texts.isEmpty()) return emptyList()
    val maxLines = policy.maxLines.coerceAtLeast(1)
    val minScale = policy.minScale.coerceIn(0.05f, 1f)
    var chosen: Float? = null
    for (scale in fitScales(minScale)) {
        val allFit =
            texts.all { text ->
                !resolveAtScale(text, slotWidthPx, scale, maxLines, widthOf).ellipsized
            }
        if (allFit) {
            chosen = scale
            break
        }
    }
    val scale = chosen ?: minScale
    return texts.map { resolveAtScale(it, slotWidthPx, scale, maxLines, widthOf) }
}

internal fun resolveAtScale(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    maxLines: Int,
    widthOf: (String, Float) -> Float,
): FitTextDecision {
    if (text.isEmpty() || maxWidthPx <= 0f) {
        return FitTextDecision(scale, text, 1, ellipsized = false)
    }
    if (fitsWidth(widthOf(text, scale), maxWidthPx)) {
        return FitTextDecision(scale, text, 1, ellipsized = false)
    }
    val wrapped = wrapLines(text, maxWidthPx, scale, maxLines, widthOf)
    if (wrapped != null) {
        val shown = if (wrapped.size == 1) wrapped[0] else wrapped.joinToString("\n")
        return FitTextDecision(scale, shown, wrapped.size, ellipsized = false)
    }
    val cut = ellipsizeLines(text, maxWidthPx, scale, maxLines, widthOf)
    val lineCount = cut.count { it == '\n' } + 1
    return FitTextDecision(scale, cut, lineCount, ellipsized = cut != text)
}

/**
 * Narrowest width at which [text] wraps into [maxLines] at [scale] without
 * an ellipsis. One line needs the full string. More lines need at least the
 * widest unbreakable piece ("OCR-Seite" may break after the hyphen, #45).
 */
internal fun minWidthForLines(
    text: String,
    scale: Float,
    maxLines: Int,
    widthOf: (String, Float) -> Float,
): Float {
    if (text.isEmpty()) return 0f
    val full = widthOf(text, scale)
    if (maxLines <= 1) return full
    val widest = widestPiece(text, scale, widthOf)
    if (widest >= full - 0.5f) return full
    if (wrapLines(text, widest, scale, maxLines, widthOf) != null) return widest
    var lo = widest
    var hi = full
    repeat(16) {
        val mid = (lo + hi) / 2f
        if (wrapLines(text, mid, scale, maxLines, widthOf) != null) hi = mid else lo = mid
    }
    return hi
}

internal enum class BarItemMode { Labelled, IconOnly, Overflow }

internal data class ActionBarPlan(
    val modes: List<BarItemMode>,
    val labelScale: Float?,
)

/**
 * Action-bar degrade steps (#45). [labelledPxAtScale] is largest-scale-first;
 * a scale is usable only when every label fits in [FitTextPolicy.maxLines]
 * without an ellipsis. The overflow button is not one of [modes]: the caller
 * always reserves [overflowPx] so that button stays on screen.
 */
internal fun planActionBar(
    availablePx: Float,
    overflowPx: Float,
    pinnedPx: Float,
    iconOnlyPx: List<Float>,
    labelledPxAtScale: List<Pair<Float, List<Float>>>,
): ActionBarPlan {
    val count = iconOnlyPx.size
    val overflowAll = ActionBarPlan(List(count) { BarItemMode.Overflow }, null)
    val room = availablePx - overflowPx - pinnedPx
    if (count == 0) return overflowAll
    if (room <= 0f) return overflowAll
    for ((scale, widths) in labelledPxAtScale) {
        if (widths.size == count && widths.sum() <= room + 0.5f) {
            return ActionBarPlan(List(count) { BarItemMode.Labelled }, scale)
        }
    }
    if (iconOnlyPx.sum() <= room + 0.5f) {
        return ActionBarPlan(List(count) { BarItemMode.IconOnly }, null)
    }
    val modes = MutableList(count) { BarItemMode.Overflow }
    var used = 0f
    for (i in iconOnlyPx.indices) {
        val width = iconOnlyPx[i]
        if (used + width <= room + 0.5f) {
            modes[i] = BarItemMode.IconOnly
            used += width
        } else {
            break
        }
    }
    return ActionBarPlan(modes, null)
}

private fun fitsWidth(
    width: Float,
    maxWidthPx: Float,
): Boolean = width <= maxWidthPx + 0.5f

private fun widestPiece(
    text: String,
    scale: Float,
    widthOf: (String, Float) -> Float,
): Float {
    var widest = 0f
    val piece = StringBuilder()

    fun consider() {
        val value = piece.toString().trim()
        if (value.isNotEmpty()) widest = maxOf(widest, widthOf(value, scale))
        piece.clear()
    }
    for (c in text) {
        piece.append(c)
        if (isFitBreakChar(c) || isCjkChar(c)) consider()
    }
    consider()
    return widest
}

/**
 * The line [text][0, end) as it is drawn when the break lands at [end]
 * (#62): a soft hyphen at the break shows as a visible hyphen and is not
 * carried to the next line.
 */
internal fun drawnLinePiece(
    text: String,
    end: Int,
): String {
    val trimmed = text.substring(0, end).trimEnd()
    return if (trimmed.endsWith('\u00AD')) trimmed.dropLast(1) + "-" else trimmed
}

internal fun wrapLines(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    maxLines: Int,
    widthOf: (String, Float) -> Float,
): List<String>? {
    if (text.isEmpty()) return emptyList()
    if (maxWidthPx <= 0f || maxLines <= 0) return null
    val lines = ArrayList<String>(maxLines)
    var remaining = text
    while (remaining.isNotEmpty()) {
        if (lines.size == maxLines) return null
        val trimmed = if (lines.isEmpty()) remaining else remaining.trimStart()
        if (trimmed.isEmpty()) return lines
        if (fitsWidth(widthOf(trimmed, scale), maxWidthPx)) {
            lines += trimmed
            return lines
        }
        val end = longestFittingEnd(trimmed, maxWidthPx, scale, widthOf)
        if (end <= 0 || end >= trimmed.length) return null
        val piece = drawnLinePiece(trimmed, end)
        if (piece.isEmpty()) return null
        lines += piece
        remaining = trimmed.substring(end)
    }
    return lines
}

private fun longestFittingEnd(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    widthOf: (String, Float) -> Float,
): Int {
    var best = 0
    for (end in breakEnds(text)) {
        if (end >= text.length) continue
        // The drawn form is what must fit: a soft hyphen at the break
        // shows as a visible hyphen (#62).
        val piece = drawnLinePiece(text, end)
        if (piece.isEmpty()) continue
        if (fitsWidth(widthOf(piece, scale), maxWidthPx)) best = end else break
    }
    return best
}

private fun breakEnds(text: String): List<Int> {
    val ends = ArrayList<Int>()
    for (i in text.indices) {
        if (isFitBreakChar(text[i])) {
            ends += i + 1
        } else if (isCjkChar(text[i]) && (i + 1 == text.length || !isCjkNoLineStartChar(text[i + 1]))) {
            // Between two CJK characters a line may start (#62): a script
            // without spaces has no word boundaries, and per-character
            // wrapping is the normal line breaking there.
            ends += i + 1
        }
    }
    if (ends.lastOrNull() != text.length) ends += text.length
    return ends
}

private fun ellipsizeLines(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    maxLines: Int,
    widthOf: (String, Float) -> Float,
): String {
    if (maxLines <= 1) return ellipsizeToWidth(text, maxWidthPx, scale, widthOf)
    val lines = ArrayList<String>(maxLines)
    var remaining = text
    while (lines.size < maxLines - 1 && remaining.isNotEmpty()) {
        val trimmed = if (lines.isEmpty()) remaining else remaining.trimStart()
        val end = longestFittingEnd(trimmed, maxWidthPx, scale, widthOf)
        if (end <= 0 || end >= trimmed.length) break
        val piece = drawnLinePiece(trimmed, end)
        if (piece.isEmpty()) break
        lines += piece
        remaining = trimmed.substring(end).trimStart()
    }
    if (remaining.isEmpty()) return lines.joinToString("\n")
    lines += ellipsizeToWidth(remaining, maxWidthPx, scale, widthOf)
    return lines.joinToString("\n")
}

private fun ellipsizeToWidth(
    text: String,
    maxWidthPx: Float,
    scale: Float,
    widthOf: (String, Float) -> Float,
): String {
    if (text.isEmpty() || fitsWidth(widthOf(text, scale), maxWidthPx)) return text
    if (!fitsWidth(widthOf(FIT_ELLIPSIS, scale), maxWidthPx)) return FIT_ELLIPSIS
    var bestEnd = 0
    for (end in breakEnds(text)) {
        if (end >= text.length) continue
        val piece = drawnLinePiece(text, end)
        if (piece.isEmpty()) continue
        if (fitsWidth(widthOf(piece + FIT_ELLIPSIS, scale), maxWidthPx)) bestEnd = end else break
    }
    if (bestEnd > 0) return drawnLinePiece(text, bestEnd) + FIT_ELLIPSIS
    val wordEnd = text.indexOfFirst { isFitBreakChar(it) }.let { if (it < 0) text.length else it }
    var count = 0
    while (count < wordEnd) {
        val candidate = text.substring(0, count + 1) + FIT_ELLIPSIS
        if (!fitsWidth(widthOf(candidate, scale), maxWidthPx)) break
        count += 1
    }
    return if (count == 0) FIT_ELLIPSIS else text.substring(0, count) + FIT_ELLIPSIS
}
