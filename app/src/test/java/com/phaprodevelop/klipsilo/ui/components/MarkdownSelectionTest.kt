@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #176: selections and markdown markers must not swallow surrounding whitespace. */
class MarkdownSelectionTest {
    @Test
    fun trimSelectionTrailingWhitespace_dropsSpaceAfterWord() {
        assertEquals(
            TextRange(0, 4),
            trimSelectionTrailingWhitespace("word next", TextRange(0, 5)),
        )
    }

    @Test
    fun trimSelectionTrailingWhitespace_dropsNewlineAndTab() {
        assertEquals(
            TextRange(0, 4),
            trimSelectionTrailingWhitespace("word \n\t", TextRange(0, 7)),
        )
    }

    @Test
    fun trimSelectionTrailingWhitespace_keepsInternalSpaces() {
        assertEquals(
            TextRange(0, 9),
            trimSelectionTrailingWhitespace("two words", TextRange(0, 9)),
        )
    }

    @Test
    fun trimSelectionTrailingWhitespace_keepsLeadingWhitespace() {
        assertEquals(
            TextRange(0, 5),
            trimSelectionTrailingWhitespace(" word", TextRange(0, 5)),
        )
    }

    @Test
    fun markdownContentRange_trimsBothEnds() {
        assertEquals(
            TextRange(2, 11),
            markdownContentRange("  two words  ", TextRange(0, 13)),
        )
    }

    @Test
    fun markdownContentRange_keepsInternalSpaces() {
        assertEquals(
            TextRange(0, 9),
            markdownContentRange("two words", TextRange(0, 9)),
        )
    }

    @Test
    fun markdownContentRange_blankSelectionIsNull() {
        assertNull(markdownContentRange("   ", TextRange(0, 3)))
        assertNull(markdownContentRange("", TextRange(0, 0)))
    }
}
