@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** #175: a multi-page PDF asks before creating one image per page. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PdfImageConfirmTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
    }

    @After
    fun tearDown() {
        if (!::app.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(app.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun requestPdfToImages_multiPage_waitsForConfirmationWithoutOutput() {
        // Arrange
        val viewModel = newViewModel(pageCount = 2)

        // Act
        viewModel.requestPdfToImages(pdfEntry())
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        val confirmation = viewModel.pdfImageConfirmation.value
        assertNotNull(confirmation)
        assertEquals(2, confirmation!!.pageCount)
        assertFalse(viewModel.isConverting.value)
        assertTrue(runBlocking { dao.getAllOnce() }.isEmpty())

        // Act: cancel
        viewModel.dismissPdfImageConfirmation()

        // Assert: nothing was converted
        assertNull(viewModel.pdfImageConfirmation.value)
        assertTrue(runBlocking { dao.getAllOnce() }.isEmpty())
    }

    private fun newViewModel(pageCount: Int): ClipboardViewModel =
        ClipboardViewModel(
            application = app,
            repository = ClipboardRepository(dao, app),
            ioDispatcher = mainDispatcherRule.dispatcher,
            pageCountProvider = { pageCount },
        )

    private fun pdfEntry(): ClipboardEntry =
        ClipboardEntry(
            id = 7L,
            contentType = ContentType.PDF,
            previewText = "multi-page",
            filePath = File(app.cacheDir, "multi-page.pdf").absolutePath,
            timestamp = 1_000L,
        )
}
