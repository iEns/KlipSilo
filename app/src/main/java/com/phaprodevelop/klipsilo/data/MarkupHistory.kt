package com.phaprodevelop.klipsilo.data

/**
 * Returns the trimmed text to commit for a draft text annotation, or null
 * when the draft is empty or whitespace-only and must be dropped (#50):
 * placing an empty text box creates no annotation, records no undo entry
 * and does not make the screen dirty (Back leaves without the save dialog).
 */
internal fun commitMarkupText(text: String): String? = text.trim().takeIf { it.isNotEmpty() }

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
