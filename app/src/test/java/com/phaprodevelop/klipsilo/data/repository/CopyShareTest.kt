package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CopyShareTest {
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
    fun copyToClipboard_missingImage_returnsFalse() =
        runTest {
            // Arrange
            val missing = File(File(context.filesDir, "images"), "gone.jpg")
            val entry = imageEntry(missing, id = 1L)

            // Act
            val copied = repo.copyToClipboard(entry)

            // Assert
            assertFalse(copied)
        }

    @Test
    fun copyToClipboard_existingImage_fileProviderOnly_noPicturesFile() =
        runTest {
            // Arrange
            val file = createStoredFile("images", "photo.jpg", jpegBytes())
            val entry = imageEntry(file, id = 2L)
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val before = pictures.listFiles()?.map { it.canonicalPath }.orEmpty().toSet()

            // Act
            val copied = repo.copyToClipboard(entry)
            val clip =
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            val after = pictures.listFiles()?.map { it.canonicalPath }.orEmpty().toSet()

            // Assert
            assertTrue(copied)
            assertEquals(before, after)
            assertNotNull(clip)
            val uri = clip!!.getItemAt(0).uri
            assertNotNull(uri)
            assertEquals("content", uri.scheme)
            assertTrue(uri.authority!!.contains("fileprovider"))
        }

    @Test
    fun copyToClipboard_emptyText_returnsTrue() =
        runTest {
            // Arrange
            val entry =
                ClipboardEntry(
                    id = 3L,
                    contentType = ContentType.TEXT,
                    textContent = "",
                    previewText = "",
                    timestamp = 1L,
                )
            repo.insertEntry(entry)

            // Act
            val copied = repo.copyToClipboard(entry)

            // Assert
            assertTrue(copied)
        }

    @Test
    fun copyToClipboard_location_includesGeoCoordinates() =
        runTest {
            // Arrange
            val entry =
                ClipboardEntry(
                    id = 4L,
                    contentType = ContentType.LOCATION,
                    previewText = "Copenhagen",
                    timestamp = 1L,
                    latitude = 55.6761,
                    longitude = 12.5683,
                )

            // Act
            val copied = repo.copyToClipboard(entry)
            val text =
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .primaryClip!!
                    .getItemAt(0)
                    .text
                    .toString()

            // Assert
            assertTrue(copied)
            assertTrue(text.startsWith("Copenhagen"))
            assertTrue(text.contains("geo:55.6761,12.5683"))
        }

    @Test
    fun shareClip_missingFile_returnsFalse() =
        runTest {
            // Arrange
            val missing = File(File(context.filesDir, "pdfs"), "gone.pdf")
            val entry =
                ClipboardEntry(
                    id = 5L,
                    contentType = ContentType.PDF,
                    filePath = missing.absolutePath,
                    previewText = "gone",
                    timestamp = 1L,
                )

            // Act
            val shared = repo.shareClip(entry)

            // Assert
            assertFalse(shared)
        }

    @Test
    fun shareText_blank_returnsFalse() {
        assertFalse(repo.shareText(""))
        assertFalse(repo.shareText("   "))
    }

    @Test
    fun shareMultipleClips_empty_returnsFalse() {
        assertFalse(repo.shareMultipleClips(emptyList()))
    }

    private fun imageEntry(
        file: File,
        id: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.IMAGE,
        textContent = null,
        filePath = file.absolutePath,
        previewText = file.nameWithoutExtension,
        timestamp = 1L,
    )

    private fun createStoredFile(
        subdir: String,
        name: String,
        content: ByteArray,
    ): File {
        val dir = File(context.filesDir, subdir).apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(content) }
    }

    private fun jpegBytes(): ByteArray {
        val bytes = ByteArray(32)
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xD8.toByte()
        bytes[2] = 0xFF.toByte()
        return bytes
    }
}
