@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHeadingTest {
    @Test
    fun compactHeadings_stayCloseToBody_andPrintHeadingsStayLarger() {
        assertEquals(1.8f, markdownHeadingFactor(1, compact = false))
        assertEquals(1f, markdownHeadingFactor(6, compact = false))
        assertTrue(markdownHeadingFactor(1, compact = true) < 1.4f)
        assertTrue(markdownHeadingFactor(1, compact = true) > markdownHeadingFactor(2, compact = true))
        assertTrue(markdownHeadingFactor(4, compact = true) > 1f)
    }
}
