package com.phaprodevelop.klipsilo.ui.components

import kotlin.math.abs

/**
 * How a label gives up space (#45 #47 #50 #54).
 *
 * 1. Draw at the style size on one line.
 * 2. Wrap at word boundaries only, up to [maxLines].
 * 3. Shrink, in [FIT_SCALE_STEP] steps, down to [minScale]. A uniform group
 *    uses one scale for every sibling.
 * 4. Ellipsis at the end of the last line. A line never starts mid-word.
 *    A single word that cannot fit is cut with an ellipsis, not broken.
 *
 * [minScale] is a fraction of the style size (1 keeps the style size).
 */
internal data class FitTextPolicy(
    val maxLines: Int = 2,
    val minScale: Float = LABEL_FONT_FLOOR_FRACTION,
)

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

/** '_' '-' '.' '/' whitespace and U+200B are break opportunities (#45 #54). */
internal fun isFitBreakChar(c: Char): Boolean =
    c.isWhitespace() || c == '-' || c == '_' || c == '.' || c == '/' || c == '\u200B'

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
 * ellipsis, otherwise [FitTextPolicy.minScale] with an end ellipsis.
 */
internal fun resolveFitText(
    text: String,
    maxWidthPx: Float,
    policy: FitTextPolicy,
    widthOf: (String, Float) -> Float,
): FitTextDecision {
    val maxLines = policy.maxLines.coerceAtLeast(1)
    val minScale = policy.minScale.coerceIn(0.05f, 1f)
    if (text.isEmpty() || maxWidthPx <= 0f) {
        return FitTextDecision(1f, text, 1, ellipsized = false)
    }
    for (scale in fitScales(minScale)) {
        val candidate = resolveAtScale(text, maxWidthPx, scale, maxLines, widthOf)
        if (!candidate.ellipsized || abs(scale - minScale) < 0.001f) return candidate
    }
    return resolveAtScale(text, maxWidthPx, minScale, maxLines, widthOf)
}

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
        if (isFitBreakChar(c)) consider()
    }
    consider()
    return widest
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
        val piece = trimmed.substring(0, end).trimEnd()
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
        val piece = text.substring(0, end).trimEnd()
        if (piece.isEmpty()) continue
        if (fitsWidth(widthOf(piece, scale), maxWidthPx)) best = end else break
    }
    return best
}

private fun breakEnds(text: String): List<Int> {
    val ends = ArrayList<Int>()
    for (i in text.indices) {
        if (isFitBreakChar(text[i])) ends += i + 1
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
        val piece = trimmed.substring(0, end).trimEnd()
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
        val piece = text.substring(0, end).trimEnd()
        if (piece.isEmpty()) continue
        if (fitsWidth(widthOf(piece + FIT_ELLIPSIS, scale), maxWidthPx)) bestEnd = end else break
    }
    if (bestEnd > 0) return text.substring(0, bestEnd).trimEnd() + FIT_ELLIPSIS
    val wordEnd = text.indexOfFirst { isFitBreakChar(it) }.let { if (it < 0) text.length else it }
    var count = 0
    while (count < wordEnd) {
        val candidate = text.substring(0, count + 1) + FIT_ELLIPSIS
        if (!fitsWidth(widthOf(candidate, scale), maxWidthPx)) break
        count += 1
    }
    return if (count == 0) FIT_ELLIPSIS else text.substring(0, count) + FIT_ELLIPSIS
}
