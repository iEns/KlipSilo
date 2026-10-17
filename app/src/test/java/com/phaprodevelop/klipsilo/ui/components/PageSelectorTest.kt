package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageSelectorTest {
    @Test
    fun `accepts page numbers within range`() {
        assertEquals(0, parsePageInput("1", 13))
        assertEquals(4, parsePageInput("5", 13))
        assertEquals(12, parsePageInput("13", 13))
    }

    @Test
    fun `rejects numbers outside the 1 to pageCount range`() {
        assertNull(parsePageInput("0", 13))
        assertNull(parsePageInput("14", 13))
        assertNull(parsePageInput("-2", 13))
    }

    @Test
    fun `rejects empty and non numeric input`() {
        assertNull(parsePageInput("", 13))
        assertNull(parsePageInput("   ", 13))
        assertNull(parsePageInput("to", 13))
        assertNull(parsePageInput("1a", 13))
        assertNull(parsePageInput("1,5", 13))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(2, parsePageInput(" 3 ", 10))
    }

    @Test
    fun `treats a missing page count as a single page`() {
        assertEquals(0, parsePageInput("1", 0))
        assertNull(parsePageInput("2", 0))
        assertEquals(0, parsePageInput("1", -5))
    }

    @Test
    fun `clamps page index into range`() {
        assertEquals(0, clampPageIndex(-3, 13))
        assertEquals(0, clampPageIndex(0, 13))
        assertEquals(12, clampPageIndex(12, 13))
        assertEquals(12, clampPageIndex(99, 13))
    }

    @Test
    fun `clamps against a single page when count is missing`() {
        assertEquals(0, clampPageIndex(5, 0))
        assertEquals(0, clampPageIndex(-1, -5))
    }
}
