package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TextKindMigrationTest {
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
        context
            .getSharedPreferences(
                ClipboardRepository.TEXT_KIND_MIGRATION_PREFS,
                Application.MODE_PRIVATE,
            ).edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun migrateTextKinds_txtFileBecomesPlainText() =
        runTest {
            // Arrange
            val file = storedFile("notes.txt", "hello")
            dao.insert(item(1L, ContentType.FILE, filePath = file.absolutePath))

            // Act
            repo.migrateTextKinds()

            // Assert
            assertEquals(ContentType.TEXT, dao.getAllOnce().single().contentType)
        }

    @Test
    fun migrateTextKinds_mdFileBecomesMarkdown() =
        runTest {
            // Arrange
            val file = storedFile("notes.md", "# Hello")
            dao.insert(item(2L, ContentType.FILE, filePath = file.absolutePath))

            // Act
            repo.migrateTextKinds()

            // Assert
            assertEquals(ContentType.MARKDOWN, dao.getAllOnce().single().contentType)
        }

    @Test
    fun migrateTextKinds_docxStaysFile() =
        runTest {
            // Arrange
            val file = storedFile("report.docx", "binary")
            dao.insert(item(3L, ContentType.FILE, filePath = file.absolutePath))

            // Act
            repo.migrateTextKinds()

            // Assert
            assertEquals(ContentType.FILE, dao.getAllOnce().single().contentType)
        }

    @Test
    fun migrateTextKinds_markdownLookingPlainTextStaysPlain() =
        runTest {
            // Arrange: no heuristics may promote TEXT to MARKDOWN.
            dao.insert(
                item(
                    4L,
                    ContentType.TEXT,
                    textContent = "# Heading\n\n- item",
                    previewText = "Heading",
                ),
            )

            // Act
            repo.migrateTextKinds()

            // Assert
            val stored = dao.getAllOnce().single()
            assertEquals(ContentType.TEXT, stored.contentType)
            assertEquals("# Heading\n\n- item", stored.textContent)
        }

    @Test
    fun migrateTextKinds_htmlTextBecomesHtmlFile() =
        runTest {
            // Arrange
            val html = "<!DOCTYPE html><html><body><p>hello</p></body></html>"
            dao.insert(
                item(
                    5L,
                    ContentType.TEXT,
                    textContent = html,
                    previewText = "HTML from notes",
                ),
            )

            // Act
            repo.migrateTextKinds()

            // Assert
            val stored = dao.getAllOnce().single()
            assertEquals(ContentType.FILE, stored.contentType)
            assertNull(stored.textContent)
            assertTrue(stored.filePath!!.endsWith(".html"))
            assertTrue(File(stored.filePath!!).exists())
            assertEquals(html, File(stored.filePath!!).readText())
        }

    @Test
    fun migrateTextKinds_runsExactlyOnce() =
        runTest {
            // Arrange
            val file = storedFile("once.txt", "hello")
            dao.insert(item(6L, ContentType.FILE, filePath = file.absolutePath))
            repo.migrateTextKinds()
            dao.insert(item(7L, ContentType.FILE, filePath = file.absolutePath))

            // Act
            repo.migrateTextKinds()

            // Assert
            assertEquals(ContentType.FILE, dao.getAllOnce().single { it.id == 7L }.contentType)
        }

    private fun storedFile(
        name: String,
        content: String,
    ): File {
        val dir = File(context.filesDir, "files").apply { mkdirs() }
        return File(dir, name).also { it.writeText(content) }
    }

    private fun item(
        id: Long,
        type: ContentType,
        textContent: String? = null,
        filePath: String? = null,
        previewText: String = "clip",
    ) = ClipboardItem(
        id = id,
        contentType = type,
        textContent = textContent,
        filePath = filePath,
        previewText = previewText,
        timestamp = id,
    )
}
