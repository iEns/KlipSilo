package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CombineTextClipsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var viewModel: ClipboardViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        viewModel =
            ClipboardViewModel(
                application = app,
                repository = ClipboardRepository(dao, app),
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @After
    fun tearDown() {
        if (!::app.isInitialized) return
        File(app.filesDir, "files").deleteRecursively()
    }

    @Test
    fun combineTextClips_allPlain_resultIsPlainText() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val first = entry(1L, ContentType.TEXT, "one")
            val second = entry(2L, ContentType.TEXT, "two")
            ClipboardRepository(dao, app).insertEntry(first)
            ClipboardRepository(dao, app).insertEntry(second)

            // Act
            viewModel.combineTextClips(listOf(first, second))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(ContentType.TEXT, combinedEntry().contentType)
        }

    @Test
    fun combineTextClips_anyMarkdown_resultIsMarkdown() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange (#127): mixed Plain + Markdown combines to Markdown.
            val plain = entry(1L, ContentType.TEXT, "one")
            val markdown = entry(2L, ContentType.MARKDOWN, "# two")
            ClipboardRepository(dao, app).insertEntry(plain)
            ClipboardRepository(dao, app).insertEntry(markdown)

            // Act
            viewModel.combineTextClips(listOf(plain, markdown))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val combined = combinedEntry()
            assertEquals(ContentType.MARKDOWN, combined.contentType)
            assertEquals("one\n\n# two", combined.textContent)
        }

    private suspend fun combinedEntry() = dao.getAllOnce().single { it.id !in setOf(1L, 2L) }

    private fun entry(
        id: Long,
        type: ContentType,
        text: String,
    ) = ClipboardEntry(
        id = id,
        contentType = type,
        textContent = text,
        filePath = null,
        previewText = text,
        timestamp = id,
    )
}
