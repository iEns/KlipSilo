@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
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
    fun resolveTextCommit_newEmptyBox_isDropped() {
        // Placing a new text box left empty must create nothing (#50).
        val empty = annotation(text = "")
        assertEquals(TextCommitDecision.Drop, resolveTextCommit(null, empty))
        assertEquals(
            TextCommitDecision.Drop,
            resolveTextCommit(null, annotation(text = "   \n\t ")),
        )
    }

    @Test
    fun resolveTextCommit_newBoxWithText_isAdded_trimmed() {
        assertEquals(
            TextCommitDecision.Add("Note"),
            resolveTextCommit(null, annotation(text = "Note")),
        )
        assertEquals(
            TextCommitDecision.Add("Note"),
            resolveTextCommit(null, annotation(text = "  Note  ")),
        )
    }

    @Test
    fun resolveTextCommit_editUnchanged_keepsItem() {
        val original = annotation(text = "Note", xFraction = 0.3f)
        assertEquals(
            TextCommitDecision.Keep,
            resolveTextCommit(original, annotation(text = "Note", xFraction = 0.3f)),
        )
        assertEquals(
            TextCommitDecision.Keep,
            resolveTextCommit(original, annotation(text = " Note ", xFraction = 0.3f)),
        )
    }

    @Test
    fun resolveTextCommit_keepsInternalBlankLines_butTrimsEnds() {
        // Blank lines between content lines are content (#172). Only leading and
        // trailing whitespace is trimmed.
        assertEquals(
            TextCommitDecision.Add("line1\n\n\nline4"),
            resolveTextCommit(null, annotation(text = "line1\n\n\nline4\n\n")),
        )
        assertEquals(
            TextCommitDecision.Add("line1\n\n\nline4"),
            resolveTextCommit(null, annotation(text = "\n\nline1\n\n\nline4")),
        )
    }

    @Test
    fun resolveTextCommit_editChangedText_replacesItem() {
        val original = annotation(text = "Note")
        assertEquals(
            TextCommitDecision.Replace("New"),
            resolveTextCommit(original, annotation(text = "New")),
        )
    }

    @Test
    fun resolveTextCommit_editClearedText_deletesItem() {
        // Clearing an existing text box must delete it, not restore it (#55).
        val original = annotation(text = "Note")
        assertEquals(TextCommitDecision.Delete, resolveTextCommit(original, annotation(text = "")))
        assertEquals(
            TextCommitDecision.Delete,
            resolveTextCommit(original, annotation(text = "   ")),
        )
    }

    @Test
    fun resolveTextCommit_editMovedOrRestyled_replacesItem() {
        // An edit that only moved or restyled the box still commits.
        val original = annotation(text = "Note", xFraction = 0.2f, yFraction = 0.2f)
        assertEquals(
            TextCommitDecision.Replace("Note"),
            resolveTextCommit(original, annotation(text = "Note", xFraction = 0.4f, yFraction = 0.2f)),
        )
        assertEquals(
            TextCommitDecision.Replace("Note"),
            resolveTextCommit(original, annotation(text = "Note", sizeFraction = 0.05f)),
        )
    }

    @Test
    fun undoAndRedo_clearTextboxDelete_restoresOldTextAndPosition() {
        // Deleting by clearing text is one undoable step (#55): Undo brings
        // the box back with its old text and position, Redo deletes again.
        val before = listOf(annotation(text = "Note", xFraction = 0.3f, yFraction = 0.4f))
        val after = emptyList<PdfAnnotation>()
        // recordMarkup() runs before the change, so the pre-delete state is
        // what undo restores.
        val history = MarkupHistory<List<PdfAnnotation>>()
        history.record(before)
        assertEquals(before, history.undo(after))
        assertEquals(after, history.redo(before))
        assertFalse(history.canRedo)
        assertTrue(history.canUndo)
    }
}

private fun annotation(
    text: String,
    xFraction: Float = 0.2f,
    yFraction: Float = 0.3f,
    sizeFraction: Float = 0.02f,
): PdfAnnotation = PdfAnnotation(0, xFraction, yFraction, text, sizeFraction)
