@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitTextTest {
    @Test
    fun fittedFontSize_keepsTheBaseSizeWhenItFits() {
        assertEquals(14f, fittedFontSize(14f) { true })
    }

    @Test
    fun fittedFontSize_stepsDownOneSpAtATimeUntilItFits() {
        // Everything above 11sp overflows.
        assertEquals(11f, fittedFontSize(14f) { it <= 11f })
        assertEquals(13f, fittedFontSize(14f) { it <= 13f })
    }

    @Test
    fun fittedFontSize_neverGoesBelowSeventyFivePercentOfTheBase() {
        val floor = 14f * LABEL_FONT_FLOOR_FRACTION
        // Only the floor itself fits: it is still returned, no ellipsis fallback.
        assertEquals(floor, fittedFontSize(14f) { it <= floor + 0.0001f })
    }

    @Test
    fun fittedFontSize_returnsNullWhenEvenTheFloorDoesNotFit() {
        assertNull(fittedFontSize(14f) { it < 14f * LABEL_FONT_FLOOR_FRACTION })
        assertNull(fittedFontSize(14f) { false })
    }

    @Test
    fun fittedFontSize_landsExactlyOnAnOddFloor() {
        // Base 9sp → floor 6.75sp, stepping produces 9, 8, 7, 6.75.
        assertEquals(6.75f, fittedFontSize(9f) { it <= 6.75f })
    }

    @Test
    fun isMidWordBreak_rejectsLineStartsAtWordBoundaries() {
        val text = "Auswahl speichern"
        assertFalse(isMidWordBreak(text, 0))
        assertFalse(isMidWordBreak(text, text.length))
        assertFalse(isMidWordBreak(text, 8)) // after "Auswahl "
    }

    @Test
    fun isMidWordBreak_detectsBreaksInsideAWord() {
        val text = "Text speichern"
        // Second line starts mid-word inside "speichern".
        assertTrue(isMidWordBreak(text, 6))
        assertTrue(isMidWordBreak("speichern", 4))
    }

    @Test
    fun isMidWordBreak_treatsMixedCaseHyphenBreaksAsMidWord() {
        // Conservative: breaking after a hyphen is also flagged so the font
        // keeps shrinking until the whole hyphenated chunk fits.
        assertTrue(isMidWordBreak("SVG-Datei", 4))
    }

    @Test
    fun scaledLabelStyle_scalesTheLineHeightInProportion() {
        val style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val scaled = scaledLabelStyle(style, 7f, 14f)
        assertEquals(7.sp, scaled.fontSize)
        assertEquals(10.sp, scaled.lineHeight)
    }

    @Test
    fun scaledLabelStyle_keepsAnUnspecifiedLineHeight() {
        val style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        val scaled = scaledLabelStyle(style, 7f, 14f)
        assertEquals(7.sp, scaled.fontSize)
        assertFalse(scaled.lineHeight.isSp)
    }
}
