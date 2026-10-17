package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screen-px layout math for form field values in the live preview (#58). The
 * typical Roboto metrics are about 0.93 above and 0.24 below the baseline.
 */
class FormFieldTextLayoutTest {
    private val ascent = 0.93f
    private val descent = 0.24f

    /** Wide enough that the width never limits the font. */
    private fun veryWideLayout(
        rectHeightPx: Float,
        multiline: Boolean = false,
    ) = formFieldTextLayout(
        rectWidthPx = 10000f,
        rectHeightPx = rectHeightPx,
        multiline = multiline,
        ascentPerEmPx = ascent,
        descentPerEmPx = descent,
    )

    @Test
    fun singleLineCentresTheGlyphBoxInTheRect() {
        // h = 100: font 70, ascent 65.1, descent 16.8.
        val layout = veryWideLayout(rectHeightPx = 100f)
        assertEquals(100f * FORM_FIELD_FONT_FILL, layout.fontSizePx, 0.01f)
        val glyphHeight = (ascent + descent) * layout.fontSizePx
        val expectedBaseline = (100f - glyphHeight) / 2f + ascent * layout.fontSizePx
        assertEquals(expectedBaseline, layout.baselinePx, 0.01f)
    }

    @Test
    fun glyphsStayInsideTheRectForSmallFields() {
        for (h in listOf(8f, 12f, 16f, 24f)) {
            val layout = veryWideLayout(rectHeightPx = h)
            val top = layout.baselinePx - ascent * layout.fontSizePx
            val bottom = layout.baselinePx + descent * layout.fontSizePx
            assertTrue("field h=$h top $top", top >= -0.5f)
            assertTrue("field h=$h bottom $bottom", bottom <= h + 0.5f)
            // The glyph box is centred, so there is as much room below as above.
            assertEquals(top, h - bottom, 0.05f)
        }
    }

    @Test
    fun glyphsStayInsideTheRectForTallFields() {
        for (h in listOf(96f, 200f, 400f)) {
            val layout = veryWideLayout(rectHeightPx = h)
            val top = layout.baselinePx - ascent * layout.fontSizePx
            val bottom = layout.baselinePx + descent * layout.fontSizePx
            assertTrue("field h=$h top $top", top >= -0.5f)
            assertTrue("field h=$h bottom $bottom", bottom <= h + 0.5f)
            assertEquals(top, h - bottom, 0.1f)
        }
    }

    @Test
    fun zoomScaleInvariance() {
        // Zoom only multiplies the rect px; the layout ratios must not change.
        val small = veryWideLayout(rectHeightPx = 40f)
        val zoomed = veryWideLayout(rectHeightPx = 40f * 3.5f)
        assertEquals(
            small.baselinePx / 40f,
            zoomed.baselinePx / (40f * 3.5f),
            0.002f,
        )
        assertEquals(
            small.fontSizePx / 40f,
            zoomed.fontSizePx / (40f * 3.5f),
            0.002f,
        )
    }

    @Test
    fun multilineValueTopAlignsWithPadding() {
        val layout =
            veryWideLayout(rectHeightPx = 120f, multiline = true)
        assertEquals(120f * FORM_FIELD_FONT_FILL, layout.fontSizePx, 0.01f)
        // The first baseline sits one ascent (+ padding) below the rect top,
        // well above the single-line centring for a tall rect.
        assertTrue(layout.baselinePx <= ascent * layout.fontSizePx + 1f)
        val centred = veryWideLayout(rectHeightPx = 120f)
        assertTrue(layout.baselinePx < centred.baselinePx)
    }

    @Test
    fun longSingleLineValueShrinksToFitTheWidth() {
        // "Amsterdam"s at 1px font is about 4.7px wide.
        val layout =
            formFieldTextLayout(
                rectWidthPx = 94f,
                rectHeightPx = 100f,
                multiline = false,
                ascentPerEmPx = ascent,
                descentPerEmPx = descent,
                textWidthPerEmPx = 4.7f,
                leftPaddingPx = 3f,
            )
        assertEquals((94f - 2f * 3f) / 4.7f, layout.fontSizePx, 0.01f)
        assertTrue(layout.fontSizePx < 100f * FORM_FIELD_FONT_FILL)
    }

    @Test
    fun shortSingleLineValueKeepsTheHeightFitSize() {
        val layout =
            formFieldTextLayout(
                rectWidthPx = 400f,
                rectHeightPx = 100f,
                multiline = false,
                ascentPerEmPx = ascent,
                descentPerEmPx = descent,
                textWidthPerEmPx = 4.7f,
                leftPaddingPx = 3f,
            )
        assertEquals(100f * FORM_FIELD_FONT_FILL, layout.fontSizePx, 0.01f)
    }

    @Test
    fun degenerateRectsStayInsideBounds() {
        val layout =
            formFieldTextLayout(
                rectWidthPx = 0f,
                rectHeightPx = 0f,
                multiline = false,
                ascentPerEmPx = ascent,
                descentPerEmPx = descent,
            )
        assertTrue(layout.fontSizePx >= 1f)
        assertTrue(layout.baselinePx >= 0f)

        val negative =
            formFieldTextLayout(
                rectWidthPx = -5f,
                rectHeightPx = -5f,
                multiline = false,
                ascentPerEmPx = ascent,
                descentPerEmPx = descent,
            )
        assertTrue(negative.fontSizePx >= 1f)
        assertTrue(negative.baselinePx.isFinite())
    }
}
