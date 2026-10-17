package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the edit-mode caret placement and touch target of a markup text
 * box (#55).
 */
class TextBoxCaretTest {
    // Fake monospace metrics: 10px per character.
    private val measure: (String) -> Float = { it.length * 10f }

    @Test
    fun emptyLines_fallBackToStart() {
        assertEquals(0, caretOffsetForTap(100f, 100f, emptyList(), 20f, measure))
    }

    @Test
    fun tapInTrailingPadding_putsCaretAtEndOfText() {
        assertEquals(5, caretOffsetForTap(1000f, 5f, listOf("Hello"), 20f, measure))
    }

    @Test
    fun tapRightOfFirstLine_putsCaretAtEndOfThatLine() {
        // Two wrapped lines; a tap right of line 1 must end line 1, not the text.
        assertEquals(5, caretOffsetForTap(1000f, 10f, listOf("Hello", "World"), 20f, measure))
    }

    @Test
    fun tapBelowLastLine_putsCaretAtEndOfText() {
        assertEquals(11, caretOffsetForTap(1000f, 1000f, listOf("Hello", "World"), 20f, measure))
    }

    @Test
    fun tapInTopOrLeftPadding_clampsToNearestOffset() {
        assertEquals(0, caretOffsetForTap(-50f, -50f, listOf("Hello", "World"), 20f, measure))
        // Top padding over line 2 lands at the start of line 2.
        assertEquals(6, caretOffsetForTap(-50f, 25f, listOf("Hello", "World"), 20f, measure))
    }

    @Test
    fun tapInsideALine_placesCaretAtNearestCharacter() {
        // Character 3 ('l') spans x 20..30; a tap at 22 sits before it.
        assertEquals(2, caretOffsetForTap(22f, 5f, listOf("Hello"), 20f, measure))
        // Just left of the character boundary (x=20) picks the earlier one.
        assertEquals(1, caretOffsetForTap(14f, 5f, listOf("Hello"), 20f, measure))
    }

    @Test
    fun tapOnZeroLineHeight_fallsBackToFirstLine() {
        assertEquals(0, caretOffsetForTap(4f, 100f, listOf("Hello", "World"), 0f, measure))
    }

    @Test
    fun insideBoxAndInsideSlop_areInsideEditArea_outsideCommits() {
        val box = Rect(100f, 100f, 200f, 150f)
        val area = expandedEditTapBounds(box, padPx = 20f, trailingPadPx = 40f, rtl = false)
        assertTrue(area.contains(Offset(150f, 125f))) // inside → edit
        assertTrue(area.contains(Offset(90f, 125f))) // inside slop → edit
        assertTrue(area.contains(Offset(230f, 125f))) // trailing padding → edit
        assertFalse(area.contains(Offset(75f, 125f))) // clearly outside → commit
        assertFalse(area.contains(Offset(250f, 125f))) // past trailing padding → commit
        assertTrue(area.contains(Offset(150f, 85f))) // above the box, inside slop → edit
        assertFalse(area.contains(Offset(150f, 75f))) // clearly above → commit
    }

    @Test
    fun extraTrailingPadding_respectsRtl() {
        val box = Rect(100f, 100f, 200f, 150f)
        val ltr = expandedEditTapBounds(box, 20f, 40f, rtl = false)
        assertEquals(80f, ltr.left)
        assertEquals(240f, ltr.right)
        val rtl = expandedEditTapBounds(box, 20f, 40f, rtl = true)
        assertEquals(60f, rtl.left)
        assertEquals(220f, rtl.right)
    }

    @Test
    fun pageAndScreenCoordinates_roundTrip() {
        val container = IntSize(400, 800)
        val point = Offset(250f, 300f)
        val screen = pageToScreen(point, container, scale = 2f, offsetX = 30f, offsetY = -20f)
        assertEquals(point, screenToPage(screen, container, scale = 2f, offsetX = 30f, offsetY = -20f))
    }

    @Test
    fun nonPositiveScale_isTreatedAsIdentity() {
        val container = IntSize(400, 800)
        val point = Offset(250f, 300f)
        assertEquals(point, pageToScreen(point, container, 0f, 0f, 0f))
        assertEquals(point, screenToPage(point, container, 0f, 0f, 0f))
    }

    @Test
    fun caretFraction_mapsTheTapIntoTheTextBlock() {
        // Text block at (0.1, 0.2)*400x600 = origin (40,120) in the page bitmap,
        // 120px wide, 60px high (#55b).
        val fraction =
            caretFractionInText(
                fx = 0.25f,
                fy = 0.25f,
                annotationXFraction = 0.1f,
                annotationYFraction = 0.2f,
                textWidthPx = 120f,
                textHeightPx = 60f,
                bitmapWidth = 400,
                bitmapHeight = 600,
            )
        // fy - 0.2 = 0.05 of a 600px-high bitmap over a 60px block reaches half its height.
        assertEquals(0.5f, fraction!!.first, 0.0001f)
        assertEquals(0.5f, fraction.second, 0.0001f)
    }

    @Test
    fun caretFraction_nullWithoutABlock() {
        assertNull(
            caretFractionInText(0.5f, 0.5f, 0f, 0f, 0f, 60f, 400, 600),
        )
        assertNull(
            caretFractionInText(0.5f, 0.5f, 0f, 0f, 120f, 60f, 0, 600),
        )
    }

    @Test
    fun caretFraction_clampsOutsideTaps() {
        val fraction =
            caretFractionInText(
                fx = 1.2f,
                fy = -0.4f,
                annotationXFraction = 0.2f,
                annotationYFraction = 0.2f,
                textWidthPx = 100f,
                textHeightPx = 50f,
                bitmapWidth = 500,
                bitmapHeight = 500,
            )
        assertEquals(1f, fraction!!.first, 0.0001f)
        assertEquals(0f, fraction.second, 0.0001f)
    }

    @Test
    fun localCaretPosition_scalesAndClamps() {
        assertEquals(Offset(50f, 25f), localCaretPosition(0.5f, 0.5f, 100f, 50f))
        // Fractions beyond the block clamp to its edge (keep the caret inside the field).
        assertEquals(Offset(100f, 0f), localCaretPosition(1.2f, -1f, 100f, 50f))
    }
}
