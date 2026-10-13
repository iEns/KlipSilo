package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SaveNewClipTest {
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun allNull_returnsFailed() =
        runTest {
            // Arrange / Act
            val result = repo.saveNewClip(null, null, null, null)

            // Assert
            assertEquals(SaveClipResult.Failed, result)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun plainText_savesTextType_previewEqualsText() =
        runTest {
            // Arrange
            val text = "hello clip"

            // Act
            val result = repo.saveNewClip(text, null, null, null)
            val item = dao.getAllOnce().single()

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.TEXT, item.contentType)
            assertEquals(text, item.textContent)
            assertEquals(text, item.previewText)
        }

    @Test
    fun textLongerThan100_previewTruncatedWithEllipsis() =
        runTest {
            // Arrange
            val text = "a".repeat(120)

            // Act
            val result = repo.saveNewClip(text, null, null, null)
            val item = dao.getAllOnce().single()

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.TEXT, item.contentType)
            assertEquals(text, item.textContent)
            assertEquals(103, item.previewText.length)
            assertTrue(item.previewText.endsWith("..."))
            assertEquals("a".repeat(100) + "...", item.previewText)
        }

    @Test
    fun webUrl_savesUrlType() =
        runTest {
            // Arrange
            val url = "https://example.com/path"

            // Act
            val result = repo.saveNewClip(url, null, null, null)
            val item = dao.getAllOnce().single()

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.URL, item.contentType)
            assertEquals(url, item.textContent)
            assertEquals(url, item.previewText)
        }

    @Test
    fun imageUri_jpegMagic_savesImageUnderImagesDir() =
        runTest {
            // Arrange
            val source = writeCacheFile("photo.jpg", jpegBytes())

            // Act
            val result = repo.saveNewClip(null, Uri.fromFile(source), null, null)
            val item = dao.getAllOnce().single()
            val stored = File(item.filePath!!)

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.IMAGE, item.contentType)
            assertTrue(stored.exists())
            assertEquals(
                File(context.filesDir, "images").canonicalPath,
                stored.parentFile!!.canonicalPath,
            )
            assertTrue(stored.name.endsWith(".jpg"))
        }

    @Test
    fun pdfUri_percentPDF_savesPdfUnderPdfsDir() =
        runTest {
            // Arrange
            val source = writeCacheFile("doc.pdf", pdfBytes())

            // Act
            val result = repo.saveNewClip(null, null, Uri.fromFile(source), null)
            val item = dao.getAllOnce().single()
            val stored = File(item.filePath!!)

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.PDF, item.contentType)
            assertTrue(stored.exists())
            assertEquals(
                File(context.filesDir, "pdfs").canonicalPath,
                stored.parentFile!!.canonicalPath,
            )
            assertTrue(stored.name.endsWith(".pdf"))
        }

    @Test
    fun fileUri_unknownBytes_savesUnderFilesDir() =
        runTest {
            // Arrange
            val source = writeCacheFile("blob.bin", ByteArray(24) { 0x11 })

            // Act
            val result = repo.saveNewClip(null, null, null, Uri.fromFile(source))
            val item = dao.getAllOnce().single()
            val stored = File(item.filePath!!)

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.FILE, item.contentType)
            assertTrue(stored.exists())
            assertEquals(
                File(context.filesDir, "files").canonicalPath,
                stored.parentFile!!.canonicalPath,
            )
        }

    @Test
    fun imageUriTakesPriorityOverText() =
        runTest {
            // Arrange
            val source = writeCacheFile("priority.jpg", jpegBytes())

            // Act
            val result =
                repo.saveNewClip(
                    "https://example.com/path",
                    Uri.fromFile(source),
                    null,
                    null,
                )
            val item = dao.getAllOnce().single()
            val stored = File(item.filePath!!)

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.IMAGE, item.contentType)
            assertTrue(stored.exists())
            assertEquals(
                File(context.filesDir, "images").canonicalPath,
                stored.parentFile!!.canonicalPath,
            )
        }

    private fun writeCacheFile(
        name: String,
        bytes: ByteArray,
    ): File {
        return File(context.cacheDir, name).also { it.writeBytes(bytes) }
    }

    private fun jpegBytes(): ByteArray {
        val bytes = ByteArray(32)
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xD8.toByte()
        bytes[2] = 0xFF.toByte()
        return bytes
    }

    private fun pdfBytes(): ByteArray = "%PDF".toByteArray() + ByteArray(16)
}
