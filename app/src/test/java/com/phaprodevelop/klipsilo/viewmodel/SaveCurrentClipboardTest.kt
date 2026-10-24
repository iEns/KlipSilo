@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

/** #101: clipboard save failures explain why and offer Retry when it can work. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveCurrentClipboardTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var viewModel: ClipboardViewModel
    private lateinit var clipboard: ClipboardManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        viewModel = newViewModel(dao)
        clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.clearPrimaryClip()
    }

    @After
    fun tearDown() {
        if (!::app.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(app.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun emptyClipboard_toastsEmptyWithoutSaving() =
        runTest(mainDispatcherRule.dispatcher) {
            // Act
            viewModel.saveCurrentClipboard()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(string(R.string.clipboard_empty), viewModel.toastMessage.value)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun blankClipboard_toastsNoSaveableContentWithoutSaving() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))

            // Act
            viewModel.saveCurrentClipboard()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(string(R.string.clipboard_no_saveable_content), viewModel.toastMessage.value)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun textClipboard_savesClipAndToastsSaved() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            clipboard.setPrimaryClip(ClipData.newPlainText("clip", "hello"))

            // Act
            viewModel.saveCurrentClipboard()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = dao.getAllOnce().single()
            assertEquals(ContentType.TEXT, item.contentType)
            assertEquals("hello", item.textContent)
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    @Test
    fun saveFailure_offersRetryAndKeepsClipboard() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val failingDao = FailingInsertDao(FakeClipboardItemDao())
            val failingViewModel = newViewModel(failingDao)
            clipboard.setPrimaryClip(ClipData.newPlainText("clip", "hello"))
            val noticeDeferred =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    failingViewModel.toastEvents.first()
                }

            // Act
            failingViewModel.saveCurrentClipboard()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val notice = noticeDeferred.await()
            assertEquals(string(R.string.clip_save_failed), notice.message)
            assertEquals(string(R.string.retry), notice.actionLabel)
            assertEquals(ClipNoticeAction.RetrySaveClipboard, notice.action)
            assertEquals("hello", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            assertTrue(failingDao.getAllOnce().isEmpty())
        }

    @Test
    fun retryAfterFailure_savesTheSameClipboardContent() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val failingDao = FailingInsertDao(FakeClipboardItemDao(), failuresRemaining = 1)
            val failingViewModel = newViewModel(failingDao)
            clipboard.setPrimaryClip(ClipData.newPlainText("clip", "hello"))
            failingViewModel.saveCurrentClipboard()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Act
            failingViewModel.retryLastSave()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = failingDao.getAllOnce().single()
            assertEquals("hello", item.textContent)
            assertEquals(string(R.string.clip_saved), failingViewModel.toastMessage.value)
        }

    private fun newViewModel(dao: ClipboardItemDao): ClipboardViewModel =
        ClipboardViewModel(
            application = app,
            repository = ClipboardRepository(dao, app),
            ioDispatcher = mainDispatcherRule.dispatcher,
        )

    private fun string(resId: Int): String = LocaleHelper.applyLocale(app).getString(resId)

    /** Lets the repository's insert fail so saveNewClip returns [SaveClipResult.Failed]. */
    private class FailingInsertDao(
        private val delegate: FakeClipboardItemDao,
        var failuresRemaining: Int = Int.MAX_VALUE,
    ) : ClipboardItemDao by delegate {
        override suspend fun insert(item: ClipboardItem): Long {
            if (failuresRemaining > 0) {
                failuresRemaining--
                throw IllegalStateException("insert failed")
            }
            return delegate.insert(item)
        }
    }
}
