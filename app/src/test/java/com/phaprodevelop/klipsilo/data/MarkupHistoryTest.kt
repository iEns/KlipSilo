@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupHistoryTest {
    @Test
    fun undoAndRedo_roundTripTextLineAndFreehandSteps() {
        val history = MarkupHistory<String>()
        history.record("empty")
        history.record("text")
        history.record("text+line")
        assertEquals("text+line", history.undo("text+line+draw"))
        assertEquals("text", history.undo("text+line"))
        assertEquals("text+line", history.redo("text"))
        assertTrue(history.canUndo)
        assertTrue(history.canRedo)
        history.record("text+line")
        assertFalse(history.canRedo)
        assertEquals("text+line", history.undo("fresh"))
    }
}
