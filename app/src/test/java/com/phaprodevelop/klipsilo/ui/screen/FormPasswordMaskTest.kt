package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormPasswordMaskTest {
    @Test
    fun `hidden password preview shows one bullet per character`() {
        assertEquals("•••••••", passwordPreviewText(isPassword = true, revealed = false, value = "hunter2"))
    }

    @Test
    fun `empty hidden password preview shows no bullets`() {
        assertEquals("", passwordPreviewText(isPassword = true, revealed = false, value = ""))
    }

    @Test
    fun `revealed password preview shows the real text`() {
        assertEquals(
            "hunter2",
            passwordPreviewText(isPassword = true, revealed = true, value = "hunter2"),
        )
    }

    @Test
    fun `plain text fields are never masked`() {
        assertEquals(
            "Anne",
            passwordPreviewText(isPassword = false, revealed = false, value = "Anne"),
        )
    }

    @Test
    fun `bullet count matches the typed length`() {
        val masked = passwordPreviewText(isPassword = true, revealed = false, value = "1234567890十二")
        assertEquals(12, masked.length)
        assertTrue(masked.all { it == '•' })
    }

    @Test
    fun `badge shows on a filled hidden password field`() {
        assertTrue(showPasswordSavedBadge(isPassword = true, revealed = false, value = "x"))
    }

    @Test
    fun `badge hides when the password is revealed`() {
        assertFalse(showPasswordSavedBadge(isPassword = true, revealed = true, value = "x"))
    }

    @Test
    fun `badge hides on empty or plain fields`() {
        assertFalse(showPasswordSavedBadge(isPassword = true, revealed = false, value = ""))
        assertFalse(showPasswordSavedBadge(isPassword = false, revealed = false, value = "x"))
    }
}
