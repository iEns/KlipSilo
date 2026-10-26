package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Camera and Clips insertion copies images into Mark up storage (#142). */
@RunWith(RobolectricTestRunner::class)
class MarkupImageImportTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    private val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = ClipboardRepository(FakeClipboardItemDao(), context)
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        File(context.cacheDir, "markup").deleteRecursively()
        File(context.filesDir, "markup_images").deleteRecursively()
    }

    @Test
    fun cameraCaptureCopySucceedsAndKeepsBytes() {
        val capture =
            File(context.cacheDir, "markup").apply { mkdirs() }
                .resolve("capture_1.jpg")
                .also { it.writeBytes(jpegBytes) }

        val copied = repo.importMarkupImageFile(capture.absolutePath)

        assertNotNull(copied)
        val copiedFile = File(copied!!)
        assertTrue(copiedFile.isFile)
        assertArrayEquals(jpegBytes, copiedFile.readBytes())
        assertEquals(File(context.filesDir, "markup_images"), copiedFile.parentFile)
        assertEquals("markup_capture_1.jpg", copiedFile.name)
        assertTrue("source capture is left in place", capture.exists())
    }

    @Test
    fun secondCopyOfSameNameGetsUniquePath() {
        val capture =
            File(context.cacheDir, "markup").apply { mkdirs() }
                .resolve("capture_2.jpg")
                .also { it.writeBytes(jpegBytes) }

        val first = repo.importMarkupImageFile(capture.absolutePath)
        val second = repo.importMarkupImageFile(capture.absolutePath)

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
        assertArrayEquals(jpegBytes, File(first!!).readBytes())
        assertArrayEquals(jpegBytes, File(second!!).readBytes())
    }

    @Test
    fun missingSourceReturnsNull() {
        val missing = File(context.cacheDir, "markup/does_not_exist.jpg")

        assertNull(repo.importMarkupImageFile(missing.absolutePath))
    }

    @Test
    fun emptySourceReturnsNull() {
        val empty =
            File(context.cacheDir, "markup").apply { mkdirs() }
                .resolve("capture_empty.jpg")
                .also { it.writeBytes(ByteArray(0)) }

        assertNull(repo.importMarkupImageFile(empty.absolutePath))
        assertTrue(File(context.filesDir, "markup_images").listFiles().isNullOrEmpty())
    }
}
