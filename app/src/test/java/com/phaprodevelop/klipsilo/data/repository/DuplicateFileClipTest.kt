@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.repository

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class DuplicateFileClipTest {
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir =
            File(System.getProperty("java.io.tmpdir"), "klipsilo-dup-${System.nanoTime()}").also {
                it.mkdirs()
            }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun samePreviewTextAndSize_matches() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.IMAGE, "report", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertEquals(1L, match?.id)
    }

    @Test
    fun samePreviewTextDifferentCase_matches() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.IMAGE, "Report", file))

        // Act
        val match = findDuplicateFileClip(clips, "REPORT.png", 32L)

        // Assert
        assertEquals(1L, match?.id)
    }

    @Test
    fun storedFileNameMatchesDisplayName_matches() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.PDF, "unrelated", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertEquals(1L, match?.id)
    }

    @Test
    fun sameSizeDifferentName_doesNotMatch() {
        // Arrange
        val file = writeFile("other.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.IMAGE, "other", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertNull(match)
    }

    @Test
    fun textClip_neverMatches() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.TEXT, "report", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertNull(match)
    }

    @Test
    fun zeroSize_doesNotMatch() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.IMAGE, "report", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 0L)

        // Assert
        assertNull(match)
    }

    @Test
    fun missingFile_doesNotMatch() {
        // Arrange
        val file = File(tempDir, "missing.png")
        val clips = listOf(entry(1L, ContentType.IMAGE, "report", file))

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertNull(match)
    }

    @Test
    fun blankDisplayName_doesNotMatch() {
        // Arrange
        val file = writeFile("report.png", bytes(32))
        val clips = listOf(entry(1L, ContentType.IMAGE, "report", file))

        // Act
        val match = findDuplicateFileClip(clips, "   ", 32L)

        // Assert
        assertNull(match)
    }

    @Test
    fun firstMatchInListOrder_wins() {
        // Arrange
        val fileA = writeFile("one_report.png", bytes(32))
        val fileB = writeFile("two_report.png", bytes(32))
        val clips =
            listOf(
                entry(7L, ContentType.IMAGE, "report", fileB),
                entry(3L, ContentType.IMAGE, "report", fileA),
            )

        // Act
        val match = findDuplicateFileClip(clips, "report.png", 32L)

        // Assert
        assertEquals(7L, match?.id)
    }

    private fun entry(
        id: Long,
        contentType: ContentType,
        previewText: String,
        file: File,
    ): ClipboardEntry =
        ClipboardEntry(
            id = id,
            contentType = contentType,
            filePath = file.absolutePath,
            previewText = previewText,
            timestamp = 0L,
        )

    private fun bytes(length: Int): ByteArray = ByteArray(length)

    private fun writeFile(
        name: String,
        content: ByteArray,
    ): File = File(tempDir, name).also { it.writeBytes(content) }
}
