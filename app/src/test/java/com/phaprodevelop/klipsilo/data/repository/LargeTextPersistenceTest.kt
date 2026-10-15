package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LargeTextPersistenceTest {
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
    fun saveNewClip_300KiB_fileBackedText_listOmitsBody() =
        runTest {
            // Arrange
            val text = "a".repeat(300 * 1024)

            // Act
            val result = repo.saveNewClip(text, null, null, null)
            val listRow = repo.getAllClips().first().single()
            val stored = dao.getAllOnce().single()
            val full = repo.getClip(listRow.id)

            // Assert
            assertEquals(SaveClipResult.Saved, result)
            assertEquals(ContentType.TEXT, listRow.contentType)
            assertNull(listRow.textContent)
            assertNull(stored.textContent)
            assertNotNull(stored.filePath)
            val file = File(stored.filePath!!)
            assertTrue(file.exists())
            assertEquals(
                File(context.filesDir, "files").canonicalPath,
                file.parentFile!!.canonicalPath,
            )
            assertEquals("a".repeat(60), listRow.previewText)
            assertEquals(text, full?.let { repo.readTextBody(it) })
            assertEquals(text, repo.readTextBody(listRow))
            assertEquals(text, file.readText())
        }

    @Test
    fun saveNewClip_9MiB_returnsTooLarge_noRow() =
        runTest {
            // Arrange
            val text = "b".repeat(9 * 1024 * 1024)

            // Act
            val result = repo.saveNewClip(text, null, null, null)

            // Assert
            assertEquals(SaveClipResult.TooLarge, result)
            assertTrue(dao.getAllOnce().isEmpty())
            assertTrue(repo.getAllClips().first().isEmpty())
            val filesDir = File(context.filesDir, "files")
            assertTrue(filesDir.listFiles().orEmpty().isEmpty())
        }

    @Test
    fun getAllClips_doesNotLeakInlineTextBody() =
        runTest {
            // Arrange
            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = "secret body",
                    filePath = null,
                    previewText = "secret body",
                    timestamp = 10L,
                )

            // Act
            repo.insertEntry(entry)
            val listRow = repo.getAllClips().first().single()

            // Assert
            assertNull(listRow.textContent)
            assertEquals("secret body", repo.getClip(1L)?.textContent)
            assertEquals("secret body", repo.readTextBody(listRow))
        }

    @Test
    fun insertEntry_oversizedText_doesNotInsert() =
        runTest {
            // Arrange
            val text = "c".repeat(9 * 1024 * 1024)
            val entry =
                ClipboardEntry(
                    id = 2L,
                    contentType = ContentType.TEXT,
                    textContent = text,
                    filePath = null,
                    previewText = "c".repeat(ClipboardRepository.MAX_PREVIEW_LENGTH) + "...",
                    timestamp = 20L,
                )

            // Act
            repo.insertEntry(entry)

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            assertTrue(repo.getAllClips().first().isEmpty())
        }

    @Test
    fun insertEntry_300KiB_fileBackedKeepsTextType() =
        runTest {
            // Arrange
            val text = "d".repeat(300 * 1024)
            val entry =
                ClipboardEntry(
                    id = 3L,
                    contentType = ContentType.URL,
                    textContent = text,
                    filePath = null,
                    previewText = text.take(ClipboardRepository.MAX_PREVIEW_LENGTH),
                    timestamp = 30L,
                )

            // Act
            repo.insertEntry(entry)
            val stored = dao.getAllOnce().single()
            val listRow = repo.getAllClips().first().single()

            // Assert
            assertEquals(ContentType.URL, stored.contentType)
            assertNull(stored.textContent)
            assertNull(listRow.textContent)
            assertNotNull(stored.filePath)
            assertTrue(File(stored.filePath!!).exists())
            assertEquals(text, repo.readTextBody(listRow))
        }
}
