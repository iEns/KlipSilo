@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/** #74: only an unzoomed horizontal swipe flips a PDF page. */
class PdfSwipeNavigationTest {
    private val threshold = 100f

    @Test
    fun `swipe left goes to the next page`() {
        assertEquals(1, pdfSwipePageStep(scale = 1f, dx = -250f, dy = 20f, thresholdPx = threshold))
    }

    @Test
    fun `swipe right goes to the previous page`() {
        assertEquals(-1, pdfSwipePageStep(scale = 1f, dx = 250f, dy = -20f, thresholdPx = threshold))
    }

    @Test
    fun `short drag does not flip`() {
        assertEquals(0, pdfSwipePageStep(scale = 1f, dx = 99f, dy = 0f, thresholdPx = threshold))
    }

    @Test
    fun `zoomed drag pans instead of flipping`() {
        assertEquals(0, pdfSwipePageStep(scale = 1.5f, dx = -400f, dy = 0f, thresholdPx = threshold))
        assertEquals(0, pdfSwipePageStep(scale = 8f, dx = -400f, dy = 0f, thresholdPx = threshold))
    }

    @Test
    fun `mostly vertical drag does not flip`() {
        assertEquals(0, pdfSwipePageStep(scale = 1f, dx = -150f, dy = 200f, thresholdPx = threshold))
        assertEquals(0, pdfSwipePageStep(scale = 1f, dx = 150f, dy = -200f, thresholdPx = threshold))
    }

    @Test
    fun `invalid input does not flip`() {
        assertEquals(0, pdfSwipePageStep(Float.NaN, -400f, 0f, threshold))
        assertEquals(0, pdfSwipePageStep(1f, Float.NaN, 0f, threshold))
        assertEquals(0, pdfSwipePageStep(1f, -400f, Float.NaN, threshold))
        assertEquals(0, pdfSwipePageStep(1f, -400f, 0f, thresholdPx = 0f))
    }
}
