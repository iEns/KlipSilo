package com.phaprodevelop.klipsilo.ui.components

/**
 * Share of the field height a hidden password's "saved as readable text" pill
 * may use (#61).
 */
const val PASSWORD_BADGE_HEIGHT_FILL = 0.8f

/**
 * Badge font as a share of the badge height (#61): small, single line, drawn
 * with a TextMeasurer so no inherited line height can clip or enlarge it.
 */
const val PASSWORD_BADGE_FONT_FILL = 0.55f

/** Horizontal padding inside the pill, per em of the badge font (#61). */
const val PASSWORD_BADGE_H_PAD_PER_EM = 0.6f

/** Gap between the dots and the pill, per em of the badge font (#61). */
const val PASSWORD_BADGE_GAP_PER_EM = 0.4f

/**
 * Smallest badge font that stays readable, in sp at the default font scale:
 * below this the preview drops the badge — the note under the input still
 * explains (#61).
 */
const val PASSWORD_BADGE_MIN_FONT_SP = 6f

/**
 * Where the hidden-password pill and its text sit inside the field rect,
 * top-down screen px (#61).
 */
internal data class PasswordBadgeLayout(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val fontSizePx: Float,
    val textLeftPx: Float,
    /** Text baseline inside the field rect, top-down, in screen px. */
    val textBaselinePx: Float,
)

/**
 * Decides whether the hidden-password pill fits beside the dots in a field rect
 * and where it goes (#61).
 *
 * The pill is centred vertically and parked at the trailing edge, with the dots
 * staying at the leading edge — the pill never covers them. Its height is
 * [PASSWORD_BADGE_HEIGHT_FILL] of the field height, its font starts at
 * [PASSWORD_BADGE_FONT_FILL] of that and shrinks so the single-line text, the
 * pill padding and the gap to the dots fit between the two edge paddings of the
 * field width. When the font would fall below [minFontPx] — or the rect simply
 * has no room — the badge is dropped (`null`); the note under the input still
 * explains.
 *
 * [badgeTextWidthPerEmPx] is the badge text's width at 1px font size;
 * [badgeFontAscentPerEm] and [badgeFontDescentPerEm] its metrics above and
 * below the baseline as fractions of the font size.
 */
internal fun passwordBadgeLayout(
    fieldWidthPx: Float,
    fieldHeightPx: Float,
    dotsWidthPx: Float,
    badgeTextWidthPerEmPx: Float,
    badgeFontAscentPerEm: Float,
    badgeFontDescentPerEm: Float,
    edgePaddingPx: Float,
    minFontPx: Float,
): PasswordBadgeLayout? {
    val width = fieldWidthPx.coerceAtLeast(0f)
    val height = fieldHeightPx.coerceAtLeast(0f)
    val pad = edgePaddingPx.coerceAtLeast(0f)
    val dotsWidth = dotsWidthPx.coerceAtLeast(0f)
    if (badgeTextWidthPerEmPx <= 0f || minFontPx <= 0f) return null
    val pillHeight = height * PASSWORD_BADGE_HEIGHT_FILL
    // Start at the height-fit size, then shrink so the pill plus the gap fits
    // the space left of the dots between the two paddings.
    val heightFitFont = pillHeight * PASSWORD_BADGE_FONT_FILL
    val freeWidth = (width - 2f * pad - dotsWidth).coerceAtLeast(0f)
    val perEm =
        badgeTextWidthPerEmPx + 2f * PASSWORD_BADGE_H_PAD_PER_EM + PASSWORD_BADGE_GAP_PER_EM
    val fontPx = minOf(heightFitFont, freeWidth / perEm)
    // Unreadable pill: drop it, the note under the input still explains (#61).
    if (fontPx < minFontPx) return null
    val textWidth = badgeTextWidthPerEmPx * fontPx
    val hPad = PASSWORD_BADGE_H_PAD_PER_EM * fontPx
    val pillWidth = textWidth + 2f * hPad
    val pillTop = (height - pillHeight) / 2f
    val glyphHeight = (badgeFontAscentPerEm + badgeFontDescentPerEm) * fontPx
    return PasswordBadgeLayout(
        leftPx = width - pad - pillWidth,
        topPx = pillTop,
        widthPx = pillWidth,
        heightPx = pillHeight,
        fontSizePx = fontPx,
        textLeftPx = width - pad - pillWidth + hPad,
        textBaselinePx = pillTop + (pillHeight - glyphHeight) / 2f + badgeFontAscentPerEm * fontPx,
    )
}
