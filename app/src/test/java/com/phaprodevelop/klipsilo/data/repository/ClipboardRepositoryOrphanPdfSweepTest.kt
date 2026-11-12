package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
class ClipboardRepositoryOrphanPdfSweepTest {
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
        File(context.filesDir, "outside.pdf").delete()
    }

    @Test
    fun sweep_deletesUnreferencedPdf() =
        runTest {
            // Arrange
            val orphan = createStoredFile("pdfs", "orphan.pdf")

            // Act
            val deleted = repo.sweepOrphanPdfs()

            // Assert
            assertEquals(1, deleted)
            assertFalse(orphan.exists())
        }

    @Test
    fun sweep_keepsPdfReferencedByLiveClip_andClipStillLoads() =
        runTest {
            // Arrange
            val bytes = "%PDF-1.4 dummy".toByteArray()
            val pdf = createStoredFile("pdfs", "keep.pdf", bytes)
            val entry = pdfEntry(pdf, id = 1L)
            repo.insertEntry(entry)

            // Act
            val deleted = repo.sweepOrphanPdfs()

            // Assert
            assertEquals(0, deleted)
            assertTrue(pdf.exists())
            assertArrayEquals(bytes, pdf.readBytes())
            val loaded = repo.getClip(entry.id)
            assertNotNull(loaded)
            assertEquals(pdf.absolutePath, loaded!!.filePath)
            assertEquals(ContentType.PDF, loaded.contentType)
        }

    @Test
    fun sweep_keepsPendingUndoPath_whenRowIsAlreadyLazilyDeleted() =
        runTest {
            // Arrange — the lazy delete removed the row, the undo window is still open.
            val pending = createStoredFile("pdfs", "pending.pdf")
            val orphan = createStoredFile("pdfs", "orphan.pdf")
            assertTrue(dao.getAllOnce().isEmpty())

            // Act
            val deleted = repo.sweepOrphanPdfs(extraReferencedPaths = listOf(pending.absolutePath))

            // Assert
            assertEquals(1, deleted)
            assertTrue(pending.exists())
            assertFalse(orphan.exists())
        }

    @Test
    fun sweep_leavesUnreferencedImagesAndFilesAlone() =
        runTest {
            // Arrange
            val image = createStoredFile("images", "orphan.jpg")
            val file = createStoredFile("files", "orphan.pdf")

            // Act
            val deleted = repo.sweepOrphanPdfs()

            // Assert
            assertEquals(0, deleted)
            assertTrue(image.exists())
            assertTrue(file.exists())
        }

    @Test
    fun sweep_leavesSubdirectoriesAndFilesOutsidePdfsDirAlone() =
        runTest {
            // Arrange
            val pdfsDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val nestedDir = File(pdfsDir, "nested").apply { mkdirs() }
            val nestedPdf = File(nestedDir, "inside.pdf").also { it.writeBytes(byteArrayOf(1, 2, 3)) }
            val outside = File(context.filesDir, "outside.pdf").also { it.writeBytes(byteArrayOf(1, 2, 3)) }

            // Act
            val deleted = repo.sweepOrphanPdfs()

            // Assert
            assertEquals(0, deleted)
            assertTrue(nestedDir.isDirectory)
            assertTrue(nestedPdf.exists())
            assertTrue(outside.exists())
        }

    @Test
    fun sweep_missingPdfsDir_returnsZero() =
        runTest {
            // Arrange
            File(context.filesDir, "pdfs").deleteRecursively()

            // Act
            val deleted = repo.sweepOrphanPdfs()

            // Assert
            assertEquals(0, deleted)
        }

    private fun pdfEntry(
        file: File,
        id: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.PDF,
        textContent = null,
        filePath = file.absolutePath,
        previewText = file.nameWithoutExtension,
        timestamp = 1_700_000_000_000L,
    )

    private fun createStoredFile(
        subdir: String,
        name: String,
        content: ByteArray = byteArrayOf(1, 2, 3, 4),
    ): File {
        val dir = File(context.filesDir, subdir).apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(content) }
    }
}
