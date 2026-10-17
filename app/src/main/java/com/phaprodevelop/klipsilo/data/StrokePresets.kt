package com.phaprodevelop.klipsilo.data

internal val STROKE_PRESETS = floatArrayOf(0.002f, 0.005f, 0.01f, 0.018f)

/**
 * Text-size ladder for the #50 stepper: the old S/M/L sizes (0.014, 0.022 and
 * 0.034 of the page height) stay on the ladder and a bit beyond both ends.
 */
internal val MARKUP_TEXT_SIZES =
    floatArrayOf(0.010f, 0.012f, 0.014f, 0.018f, 0.022f, 0.026f, 0.030f, 0.034f)

/**
 * Stroke-width ladder for the #50 stepper: the old S/M/L presets (0.003, 0.008
 * and 0.016 of the page height) stay on the ladder and a bit beyond both ends.
 */
internal val MARKUP_WIDTHS =
    floatArrayOf(0.003f, 0.004f, 0.005f, 0.006f, 0.008f, 0.010f, 0.012f, 0.016f, 0.020f, 0.024f)

internal fun nearestStrokePreset(width: Float): Float = STROKE_PRESETS.minBy { kotlin.math.abs(it - width) }

internal fun nearestMarkupWidth(width: Float): Float = MARKUP_WIDTHS.minBy { kotlin.math.abs(it - width) }

internal fun nearestMarkupTextSize(size: Float): Float = MARKUP_TEXT_SIZES.minBy { kotlin.math.abs(it - size) }

internal fun markupWidthStepCount(): Int = MARKUP_WIDTHS.size

internal fun markupTextSizeStepCount(): Int = MARKUP_TEXT_SIZES.size

/** Index of the ladder step closest to [width]. */
internal fun markupWidthStepIndex(width: Float): Int = nearestLadderIndex(MARKUP_WIDTHS, width)

/** Index of the ladder step closest to [size]. */
internal fun markupTextSizeStepIndex(size: Float): Int = nearestLadderIndex(MARKUP_TEXT_SIZES, size)

/** Ladder value at [index], clamped to the ladder bounds. */
internal fun markupWidthStep(index: Int): Float = ladderStep(MARKUP_WIDTHS, index)

/** Ladder value at [index], clamped to the ladder bounds. */
internal fun markupTextSizeStep(index: Int): Float = ladderStep(MARKUP_TEXT_SIZES, index)

private fun ladderStep(
    ladder: FloatArray,
    index: Int,
): Float = ladder[index.coerceIn(0, ladder.size - 1)]

private fun nearestLadderIndex(
    ladder: FloatArray,
    value: Float,
): Int {
    var best = 0
    for (index in ladder.indices) {
        if (kotlin.math.abs(ladder[index] - value) < kotlin.math.abs(ladder[best] - value)) best = index
    }
    return best
}
