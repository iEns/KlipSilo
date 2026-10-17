package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hidden-password pill placement inside the preview field rect (#61). Roboto
 * metrics are about 0.93 above and 0.24 below the baseline; the pill text is
 * about 4.6px per em wide for "Lesbarer Text".
 */
class PasswordBadgeLayoutTest {
    private val badgeAscent = 0.93f
    private val badgeDescent = 0.24f
    private val perEm = 4.6f
    private val pad = 2f

    /** A roomy field whose dots never crowd the pill. */
    private fun roomy(
        width: Float = 400f,
        height: Float = 60f,
        dots: Float = 60f,
    ) = passwordBadgeLayout(
        fieldWidthPx = width,
        fieldHeightPx = height,
        dotsWidthPx = dots,
        badgeTextWidthPerEmPx = perEm,
        badgeFontAscentPerEm = badgeAscent,
        badgeFontDescentPerEm = badgeDescent,
        edgePaddingPx = pad,
        minFontPx = 6f,
    )

    @Test
    fun roomyFieldPutsTheBadgeBesideTheDots() {
        val layout = roomy() ?: error("badge must fit in a roomy field")
        // Trailing edge, inside the right padding; the dots keep the lead.
        assertEquals(400f - pad, layout.leftPx + layout.widthPx, 0.01f)
        assertTrue(layout.leftPx >= 60f)
        // Vertically centred with slack above and below.
        assertTrue(layout.topPx > 0f)
        assertTrue(layout.topPx + layout.heightPx < 60f - 1f)
    }

    @Test
    fun narrowFieldDropsTheBadge() {
        // Dots leave no room for even the minimum font.
        assertNull(roomy(width = 30f, dots = 26f))
    }

    @Test
    fun badgeFontNeverExceedsTheHeightFitSize() {
        val layout = roomy(height = 60f)!!
        assertEquals(
            60f * PASSWORD_BADGE_HEIGHT_FILL * PASSWORD_BADGE_FONT_FILL,
            layout.fontSizePx,
            0.01f,
        )
    }

    @Test
    fun tallNarrowFieldShrinksTheFontToFitTheWidth() {
        // Plenty of height but barely any width left beside the dots: the font
        // must shrink instead of growing with the height.
        val layout = roomy(width = 60f, height = 200f, dots = 4f)!!
        val expected =
            (60f - 2f * pad - 4f) /
                (perEm + 2f * PASSWORD_BADGE_H_PAD_PER_EM + PASSWORD_BADGE_GAP_PER_EM)
        assertEquals(
            minOf(200f * PASSWORD_BADGE_HEIGHT_FILL * PASSWORD_BADGE_FONT_FILL, expected),
            layout.fontSizePx,
            0.01f,
        )
        // Pill fits the rect.
        assertTrue(layout.leftPx >= pad + 4f)
        assertTrue(layout.leftPx + layout.widthPx <= 60f + 0.01f)
    }

    @Test
    fun shortFieldKeepsTheBadgeInsideTheRect() {
        // Very short rect: pill height 80% of it, text still inside.
        val layout = roomy(height = 14f) ?: error("14px field should keep the badge")
        assertTrue(layout.topPx >= 0f)
        assertTrue(layout.topPx + layout.heightPx <= 14f + 0.01f)
        val top = layout.textBaselinePx - badgeAscent * layout.fontSizePx
        val bottom = layout.textBaselinePx + badgeDescent * layout.fontSizePx
        assertTrue("glyph top $top", top >= -0.5f)
        assertTrue("glyph bottom $bottom", bottom <= 14f + 0.5f)
    }

    @Test
    fun zoomScaleInvariance() {
        val small =
            passwordBadgeLayout(
                fieldWidthPx = 300f,
                fieldHeightPx = 40f,
                dotsWidthPx = 80f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 6f,
            )!!
        val zoomed =
            passwordBadgeLayout(
                fieldWidthPx = 300f * 2.5f,
                fieldHeightPx = 40f * 2.5f,
                dotsWidthPx = 80f * 2.5f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad * 2.5f,
                minFontPx = 6f,
            )!!
        assertEquals(small.leftPx / 300f, zoomed.leftPx / (300f * 2.5f), 0.002f)
        assertEquals(small.topPx / 40f, zoomed.topPx / (40f * 2.5f), 0.002f)
        assertEquals(small.widthPx / 300f, zoomed.widthPx / (300f * 2.5f), 0.002f)
        // Min font is in screen px here, so the zoomed sizes only need the
        // same shape, not the same drop decision.
        assertEquals(small.fontSizePx * 2.5f, zoomed.fontSizePx, 0.05f)
    }

    @Test
    fun theFontStopsAtTheReadableMinimum() {
        // Exactly the width where the font would fall below the readable
        // minimum: the badge goes entirely, never getting so small it sits
        // on the dots.
        val perEmTotal = perEm + 2f * PASSWORD_BADGE_H_PAD_PER_EM + PASSWORD_BADGE_GAP_PER_EM
        val justAbove =
            passwordBadgeLayout(
                fieldWidthPx = 2f * pad + 26f + 6.02f * perEmTotal,
                fieldHeightPx = 40f,
                dotsWidthPx = 26f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 6f,
            ) ?: error("badge just above the minimum font must be kept")
        assertTrue("font ${justAbove.fontSizePx}", justAbove.fontSizePx >= 6f)
        assertNull(
            passwordBadgeLayout(
                fieldWidthPx = 2f * pad + 26f + 5.98f * perEmTotal,
                fieldHeightPx = 40f,
                dotsWidthPx = 26f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 6f,
            ),
        )
    }

    @Test
    fun degenerateInputsDropTheBadge() {
        assertNull(
            passwordBadgeLayout(
                fieldWidthPx = 0f,
                fieldHeightPx = 0f,
                dotsWidthPx = 0f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 6f,
            ),
        )
        assertNull(
            passwordBadgeLayout(
                fieldWidthPx = 400f,
                fieldHeightPx = 60f,
                dotsWidthPx = 60f,
                badgeTextWidthPerEmPx = 0f,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 6f,
            ),
        )
        assertNull(
            passwordBadgeLayout(
                fieldWidthPx = 400f,
                fieldHeightPx = 60f,
                dotsWidthPx = 60f,
                badgeTextWidthPerEmPx = perEm,
                badgeFontAscentPerEm = badgeAscent,
                badgeFontDescentPerEm = badgeDescent,
                edgePaddingPx = pad,
                minFontPx = 0f,
            ),
        )
    }
}
