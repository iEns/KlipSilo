@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Info/About version line breaks after '+' and '-' (#62). */
class AboutVersionTest {
    @Test
    fun insertsDisplayBreaksAfterPlusAndMinus() {
        val version = "1.2.0-beta.4-debug+1567c74"
        assertEquals(
            "1.2.0-\u200Bbeta.4-\u200Bdebug+\u200B1567c74",
            version.insertDisplayBreaksAfterPlusAndMinus(),
        )
    }

    @Test
    fun keepsTheTokenWhenThereIsNothingToBreakAfter() {
        assertEquals("1.0", "1.0".insertDisplayBreaksAfterPlusAndMinus())
    }

    @Test
    fun leadingSeparatorKeepsItsBreak() {
        assertEquals("+\u200Bbeta", "+beta".insertDisplayBreaksAfterPlusAndMinus())
    }
}
