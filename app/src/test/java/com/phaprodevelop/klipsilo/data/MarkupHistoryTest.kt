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

    @Test
    fun undo_pendingStroke_removesOnlyThatStroke_andRedoRestoresInOrder() {
        val history = MarkupHistory<String>()
        history.record("empty")
        val afterLine = history.undoOne(committed = "text", pendingDocument = "text+line")
        assertEquals("text", afterLine)
        assertEquals("empty", history.undoOne(committed = "text"))
        assertEquals("text", history.redo("empty"))
        assertEquals("text+line", history.redo("text"))
        assertFalse(history.canRedo)
        assertTrue(history.canUndo)
    }

    @Test
    fun commitMarkupText_dropsEmptyAndWhitespaceOnlyTextBoxes() {
        // Placing an empty text box must not create an item (#50).
        assertEquals(null, commitMarkupText(""))
        assertEquals(null, commitMarkupText("   "))
        assertEquals(null, commitMarkupText("\n\t "))
    }

    @Test
    fun commitMarkupText_trimsRealText() {
        assertEquals("Note", commitMarkupText("Note"))
        assertEquals("Note", commitMarkupText("  Note  "))
        assertEquals("a b", commitMarkupText(" a b "))
    }
}
