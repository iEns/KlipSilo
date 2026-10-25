package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #120: keep the Save clipboard button once the library has clips. */
class SaveClipboardButtonTest {
    @Test
    fun emptyLibraryWithEmptyClipboardHidesTheButton() {
        assertFalse(showSaveClipboardButton(clipboardHasContent = false, clipCount = 0))
    }

    @Test
    fun emptyLibraryWithClipboardContentShowsTheButton() {
        assertTrue(showSaveClipboardButton(clipboardHasContent = true, clipCount = 0))
    }

    @Test
    fun clipsWithEmptyClipboardKeepTheDisabledButton() {
        assertTrue(showSaveClipboardButton(clipboardHasContent = false, clipCount = 3))
    }

    @Test
    fun clipsWithClipboardContentShowTheButton() {
        assertTrue(showSaveClipboardButton(clipboardHasContent = true, clipCount = 3))
    }
}
