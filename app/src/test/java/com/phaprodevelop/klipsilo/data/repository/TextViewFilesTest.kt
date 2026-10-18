@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.repository

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The detail screen's file-viewer text extraction (#66): a file without
 * usable text (binary, too large, too long) returns null so the viewer
 * falls back to the info card instead of crashing.
 */
@RunWith(RobolectricTestRunner::class)
class TextViewFilesTest {
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "klipsilo-textview-${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        if (::tempDir.isInitialized) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun plainTextFileComesThroughWhole() {
        val file = File(tempDir, "notes.txt")
        file.writeText("hello\nworld")
        assertEquals("hello\nworld", textViewFileText(file.absolutePath))
    }

    @Test
    fun missingFileHasNoText() {
        assertNull(readTextViewPlain(File(tempDir, "gone.txt").absolutePath))
    }

    @Test
    fun binaryFileHasNoText() {
        val file = File(tempDir, "app.bin")
        file.writeBytes(ByteArray(64) { it.toByte() }.also { it[0] = 0 })
        assertNull(textViewFileText(file.absolutePath))
    }

    @Test
    fun corruptZipFileHasNoText() {
        // A DOCX the zip layer cannot read: no text, no throw (#66).
        val file = File(tempDir, "broken.docx")
        file.writeBytes(ByteArray(256) { (it % 251).toByte() })
        assertNull(textViewFileText(file.absolutePath))
    }

    @Test
    fun fileOverTheByteCapHasNoText() {
        // Over MAX_TEXT_VIEW_BYTES: the viewer shows the info card.
        val oversized = File(tempDir, "over.txt")
        oversized.outputStream().use { out ->
            out.write(ByteArray(MAX_TEXT_VIEW_BYTES.toInt() + 1) { 'a'.code.toByte() })
        }
        assertNull(textViewFileText(oversized.absolutePath))
    }

    @Test
    fun plainTextOverTheCharCapIsTruncatedWithAnEllipsisLine() {
        val capped = cappedTextViewText("x".repeat(MAX_TEXT_VIEW_CHARS + 1))
        assertEquals(MAX_TEXT_VIEW_CHARS, capped.length - "\n…".length)
        assertEquals("\n…", capped.takeLast(2))
        assertEquals("short", cappedTextViewText("short"))
    }
}
