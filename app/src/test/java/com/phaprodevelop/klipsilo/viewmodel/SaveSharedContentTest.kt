@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SaveSharedContentTest {
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
        listOf("images", "pdfs", "files").forEach { name ->
            File(app.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun noTextNoStream_doesNotInsert() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val intent = Intent()

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
        }

    @Test
    fun extraText_savesTextClipAndToastsSaved() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val intent = Intent().putExtra(Intent.EXTRA_TEXT, "hello share")

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = dao.getAllOnce().single()
            assertEquals(ContentType.TEXT, item.contentType)
            assertEquals("hello share", item.textContent)
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    @Test
    fun extraTextHttpsUrl_savesUrlType() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val url = "https://example.com/path"
            val intent = Intent().putExtra(Intent.EXTRA_TEXT, url)

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = dao.getAllOnce().single()
            assertEquals(ContentType.URL, item.contentType)
            assertEquals(url, item.textContent)
        }

    @Test
    fun extraStreamAndText_savesTwoClips() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val image =
                File(app.cacheDir, "share-photo.png").also {
                    it.writeBytes(byteArrayOf(1, 2, 3, 4))
                }
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(image))
                    .putExtra(Intent.EXTRA_TEXT, "caption text")
                    .setType("*/*")

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val items = dao.getAllOnce()
            assertEquals(2, items.size)
            assertTrue(items.any { it.contentType == ContentType.IMAGE })
            assertTrue(items.any { it.contentType == ContentType.TEXT && it.textContent == "caption text" })
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    private fun string(resId: Int): String = LocaleHelper.applyLocale(app).getString(resId)
}
