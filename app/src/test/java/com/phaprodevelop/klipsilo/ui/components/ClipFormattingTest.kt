@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import com.phaprodevelop.klipsilo.data.db.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ClipFormattingTest {
    @Test
    fun clipInfoTypeLabel_spilledPlainText_usesTypeNameNotTxt() {
        // #127: a large Plain Text clip is stored in clip.txt; the list must
        // say "Text", never the storage extension.
        assertEquals(
            "Text",
            clipInfoTypeLabel(ContentType.TEXT, "/data/files/clip.txt", "Text"),
        )
    }

    @Test
    fun clipInfoTypeLabel_spilledMarkdown_usesTypeNameNotMd() {
        assertEquals(
            "Markdown",
            clipInfoTypeLabel(ContentType.MARKDOWN, "/data/files/clip.md", "Markdown"),
        )
    }

    @Test
    fun clipInfoTypeLabel_fileClip_keepsExtension() {
        assertEquals(
            "DOCX",
            clipInfoTypeLabel(ContentType.FILE, "/data/files/report.docx", "File"),
        )
    }

    @Test
    fun formatSize_zero() {
        // Arrange
        val bytes = 0L

        // Act
        val formatted = formatSize(bytes)

        // Assert
        assertEquals("0 B", formatted)
    }

    @Test
    fun formatSize_1023() {
        // Arrange
        val bytes = 1023L

        // Act
        val formatted = formatSize(bytes)

        // Assert
        assertEquals("1023 B", formatted)
    }

    @Test
    fun formatSize_1024() {
        // Arrange
        val bytes = 1024L

        // Act
        val formatted = formatSize(bytes)

        // Assert
        assertEquals("1.0 KB", formatted)
    }

    @Test
    fun formatSize_1536() {
        // Arrange
        val bytes = 1536L

        // Act
        val formatted = formatSize(bytes)

        // Assert
        assertEquals("1.5 KB", formatted)
    }

    @Test
    fun formatSize_1048576() {
        // Arrange
        val bytes = 1_048_576L

        // Act
        val formatted = formatSize(bytes)

        // Assert
        assertEquals("1.0 MB", formatted)
    }

    @Test
    fun formatSize_usesUsLocaleDot() {
        val previousDefault = Locale.getDefault()
        try {
            // Arrange
            Locale.setDefault(Locale.forLanguageTag("da-DK"))
            val bytes = 1536L

            // Act
            val formatted = formatSize(bytes)

            // Assert
            assertTrue(formatted.contains('.'))
            assertFalse(formatted.contains(','))
            assertEquals("1.5 KB", formatted)
        } finally {
            Locale.setDefault(previousDefault)
        }
    }
}
