@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfOcrPageJoinerTest {
    private fun separator(page: Int): String = "--- Page $page ---"

    @Test
    fun singlePage_hasNoPageMarker() {
        assertEquals("only page", joinOcrPageTexts(listOf("only page"), ::separator))
    }

    @Test
    fun multiplePages_getLocalizedMarkersInOrder() {
        assertEquals(
            "--- Page 1 ---\nfirst\n\n--- Page 2 ---\nsecond",
            joinOcrPageTexts(listOf("first", "second"), ::separator),
        )
    }

    @Test
    fun blankPage_isSkippedButLaterMarkerKeepsTheRealPageNumber() {
        assertEquals(
            "--- Page 1 ---\nfirst\n\n--- Page 3 ---\nthird",
            joinOcrPageTexts(listOf("first", null, "third")) { page ->
                "--- Page $page ---"
            },
        )
    }

    @Test
    fun noRecognizedText_returnsNull() {
        assertNull(joinOcrPageTexts(listOf(null, "", "  \n"), ::separator))
    }
}
