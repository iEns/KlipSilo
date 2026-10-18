package com.phaprodevelop.klipsilo.ui.components

/**
 * Font size and baseline for one field value drawn over its widget rect on
 * screen, mirroring how the saved PDF centres values in the field box.
 */
internal data class FormFieldTextLayout(
    val fontSizePx: Float,
    /** Baseline y inside the rect, top-down, in screen px. */
    val baselinePx: Float,
    val leftPaddingPx: Float,
)

/**
 * Share of the field height the value's font takes up, like the saved output's
 * auto size which fills the box vertically.
 */
const val FORM_FIELD_FONT_FILL = 0.7f

/**
 * maxLines for the live preview's value layout: single-line values get one line,
 * multiline values wrap without a cap. Compose rejects maxLines = 0 ("maxLines
 * should be greater than 0") — that rejection was the crash on the first character
 * typed in a multi-line field (#68), so the rule never returns 0.
 */
internal fun formOverlayMaxLines(multiline: Boolean): Int = if (multiline) Int.MAX_VALUE else 1

/**
 * Turns a field rect (screen px) plus the value font's metrics into the font
 * size, baseline and left padding for drawing the value in the live preview.
 *
 * Single-line fields are vertically centred: the glyph box (ascent + descent)
 * is centred in the rect, so the baseline sits at
 * `(h − (ascent + descent)) / 2 + ascent` below the rect top. Multiline fields
 * are top-aligned with padding. The font starts at [FORM_FIELD_FONT_FILL] of
 * the rect height and shrinks so the whole single-line value fits the rect
 * width, the way the saved PDF's auto font size does.
 *
 * [ascentPerEmPx] and [descentPerEmPx] are the font's ascent above and descent
 * below the baseline as fractions of the font size; [textWidthPerEmPx] is the
 * value's full width at 1px font size (0 means unknown, no width shrink).
 */
internal fun formFieldTextLayout(
    rectWidthPx: Float,
    rectHeightPx: Float,
    multiline: Boolean,
    ascentPerEmPx: Float,
    descentPerEmPx: Float,
    textWidthPerEmPx: Float = 0f,
    leftPaddingPx: Float = 0f,
): FormFieldTextLayout {
    val height = rectHeightPx.coerceAtLeast(0f)
    val pad = leftPaddingPx.coerceIn(0f, (rectWidthPx / 2f).coerceAtLeast(0f))
    var fontPx = height * FORM_FIELD_FONT_FILL
    if (!multiline && textWidthPerEmPx > 0f) {
        // The value must fit between the two paddings, like the saved output.
        val fitWidth = rectWidthPx - 2f * pad
        if (fitWidth > 0f && textWidthPerEmPx > 0f) {
            fontPx = minOf(fontPx, fitWidth / textWidthPerEmPx)
        }
    }
    fontPx = fontPx.coerceAtLeast(1f)
    val ascentPx = ascentPerEmPx * fontPx
    val descentPx = descentPerEmPx * fontPx
    val baselinePx =
        if (multiline) {
            ascentPx + pad
        } else {
            (height - (ascentPx + descentPx)) / 2f + ascentPx
        }
    return FormFieldTextLayout(
        fontSizePx = fontPx,
        baselinePx = baselinePx,
        leftPaddingPx = pad,
    )
}
