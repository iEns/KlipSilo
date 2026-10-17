package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation

/**
 * What committing a draft text box does, decided by [resolveTextCommit].
 */
internal sealed class TextCommitDecision {
    /** A new box left empty creates nothing (#50). */
    object Drop : TextCommitDecision()

    /** A new box with text is added. */
    data class Add(val text: String) : TextCommitDecision()

    /** An edit that changed nothing keeps the original item. */
    object Keep : TextCommitDecision()

    /** An edit with changed text or geometry replaces the original item. */
    data class Replace(val text: String) : TextCommitDecision()

    /** An edit cleared to empty text deletes the original item (#55). */
    object Delete : TextCommitDecision()
}

/**
 * One pure decision for committing a draft text box, which the screen then
 * applies identically for every commit path (tap elsewhere, IME Done, tool
 * switch, page change, Save):
 * - a new box ([original] null) with empty/whitespace-only text is dropped
 *   (#50): no annotation, no undo entry, screen not dirty;
 * - a new box with text is added;
 * - an edit that changed nothing keeps the original item (no undo entry,
 *   screen not dirty);
 * - an edit cleared down to empty/whitespace-only text deletes the original
 *   item (#55) as one undoable step (Undo restores the box with its old
 *   text and position);
 * - any other edit replaces the original item.
 *
 * [edited] carries the draft's full new state (text and geometry), so an
 * edit that only moved or restyled the box still commits as Replace.
 */
internal fun resolveTextCommit(
    original: PdfAnnotation?,
    edited: PdfAnnotation,
): TextCommitDecision {
    val trimmed = edited.text.trim()
    return when {
        original == null && trimmed.isEmpty() -> TextCommitDecision.Drop
        original == null -> TextCommitDecision.Add(trimmed)
        trimmed.isEmpty() -> TextCommitDecision.Delete
        trimmed == original.text &&
            original.copy(text = trimmed) == edited.copy(text = trimmed) -> TextCommitDecision.Keep
        else -> TextCommitDecision.Replace(trimmed)
    }
}

/**
 * Undo and redo for markup. [record] stores the state from before a change.
 * [undo] and [redo] take the current state and return the state to show.
 */
internal class MarkupHistory<T>(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(before: T) {
        undoStack.addLast(before)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: T): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    /**
     * One undo removes only the newest action.
     *
     * [pendingDocument] is the document with an in-progress stroke included.
     * That stroke is recorded as its own step and then undone, so earlier
     * steps stay and redo can put the stroke back.
     */
    fun undoOne(
        committed: T,
        pendingDocument: T? = null,
    ): T? {
        if (pendingDocument != null) {
            record(committed)
            return undo(pendingDocument)
        }
        return undo(committed)
    }
}
