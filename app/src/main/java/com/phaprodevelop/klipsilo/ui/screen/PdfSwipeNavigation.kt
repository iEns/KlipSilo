package com.phaprodevelop.klipsilo.ui.screen

import kotlin.math.abs

/** At or below this scale the PDF viewer counts as unzoomed and may flip pages. */
internal const val PDF_UNZOOMED_MAX_SCALE = 1.01f

/**
 * Page step for a finished one-finger drag in the PDF viewer (#74):
 * -1 for the previous page (swipe right), 1 for the next page (swipe left),
 * and 0 when the viewer is zoomed, the drag is too short, or the drag is
 * mostly vertical. Pure; unit tested.
 */
internal fun pdfSwipePageStep(
    scale: Float,
    dx: Float,
    dy: Float,
    thresholdPx: Float,
): Int {
    if (scale.isNaN() || dx.isNaN() || dy.isNaN()) return 0
    if (scale > PDF_UNZOOMED_MAX_SCALE) return 0
    if (thresholdPx <= 0f) return 0
    if (abs(dx) < thresholdPx) return 0
    if (abs(dx) <= abs(dy)) return 0
    return if (dx < 0f) 1 else -1
}
