package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TextClipKindTest {
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
        File(context.filesDir, "files").deleteRecursively()
    }

    @Test
    fun setTextClipKind_spilledPlainText_renamesBodyToMd() =
        runTest {
            // Arrange
            val body = "a".repeat(300 * 1024)
            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = body,
                    previewText = "long note",
                    timestamp = 1L,
                )
            repo.insertEntry(entry)
            val stored = repo.getClip(1L)!!
            assertTrue(stored.filePath!!.endsWith(".txt"))

            // Act
            val changed = repo.setTextClipKind(stored, ContentType.MARKDOWN)

            // Assert
            assertTrue(changed)
            val updated = repo.getClip(1L)!!
            assertEquals(ContentType.MARKDOWN, updated.contentType)
            assertTrue(updated.filePath!!.endsWith(".md"))
            assertTrue(File(updated.filePath!!).exists())
            assertEquals(body, repo.readTextBody(updated))
            assertTrue(dao.getAllOnce().single().filePath!!.endsWith(".md"))
        }

    @Test
    fun setTextClipKind_editorBody_persistsBodyAndKind() =
        runTest {
            // Arrange
            val stored =
                ClipboardEntry(
                    id = 2L,
                    contentType = ContentType.TEXT,
                    textContent = "old",
                    previewText = "note",
                    timestamp = 2L,
                )
            repo.insertEntry(stored)

            // Act
            val changed = repo.setTextClipKind(stored, ContentType.MARKDOWN, "# new body")

            // Assert
            assertTrue(changed)
            val updated = repo.getClip(2L)!!
            assertEquals(ContentType.MARKDOWN, updated.contentType)
            assertEquals("# new body", updated.textContent)
            assertNotNull(updated.textContent)
        }
}
