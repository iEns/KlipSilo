@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveSharedContentTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repository: ClipboardRepository
    private lateinit var viewModel: ClipboardViewModel
    private lateinit var clipboard: ClipboardManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repository = ClipboardRepository(dao, app)
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
    fun noTextNoStream_explainsWithoutRetryAndDoesNotInsert() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val intent = Intent()
            val noticeDeferred =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.toastEvents.first()
                }

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val notice = noticeDeferred.await()
            assertEquals(string(R.string.share_no_saveable_content), notice.message)
            assertNull(notice.actionLabel)
            assertNull(notice.action)
            assertTrue(dao.getAllOnce().isEmpty())
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
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(image))
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

    @Test
    fun duplicateImageShare_noText_doesNotInsertOrToast_showsOffer() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val existingId = 101L
            val stored =
                File(app.filesDir, "images").also {
                    it.mkdirs()
                }.resolve("report.png")
            stored.writeBytes(ByteArray(48) { 7 })
            repository.insertEntry(
                ClipboardEntry(
                    id = existingId,
                    contentType = ContentType.IMAGE,
                    filePath = stored.absolutePath,
                    previewText = "report",
                    timestamp = 0L,
                ),
            )
            val shared =
                File(app.cacheDir, "report.png").also {
                    it.writeBytes(ByteArray(48) { 7 })
                }
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shared))
                    .setType("image/png")

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(1, dao.getAllOnce().size)
            val offer = viewModel.duplicateShare.value
            assertEquals(existingId, offer?.existingId)
            assertEquals("report.png", offer?.displayName)
            assertNull(viewModel.toastMessage.value)
        }

    @Test
    fun duplicateFileShare_saveDuplicateAgain_insertsSecondClip() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val existingId = 202L
            val stored =
                File(app.filesDir, "files").also {
                    it.mkdirs()
                }.resolve("blob.bin")
            stored.writeBytes(ByteArray(21) { 3 })
            repository.insertEntry(
                ClipboardEntry(
                    id = existingId,
                    contentType = ContentType.FILE,
                    filePath = stored.absolutePath,
                    previewText = "blob",
                    timestamp = 0L,
                ),
            )
            val shared =
                File(app.cacheDir, "blob.bin").also {
                    it.writeBytes(ByteArray(21) { 3 })
                }
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shared))
                    .setType("*/*")
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()
            assertEquals(1, dao.getAllOnce().size)

            // Act
            viewModel.saveDuplicateAgain()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val items = dao.getAllOnce()
            assertEquals(2, items.size)
            assertTrue(items.any { it.contentType == ContentType.FILE })
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
            assertNull(viewModel.duplicateShare.value)
        }

    @Test
    fun duplicatePdfShare_openExistingShare_navigatesToExistingAndClearsOffer() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val existingId = 303L
            val stored =
                File(app.filesDir, "pdfs").also {
                    it.mkdirs()
                }.resolve("doc.pdf")
            stored.writeBytes("%PDF".toByteArray() + ByteArray(16))
            repository.insertEntry(
                ClipboardEntry(
                    id = existingId,
                    contentType = ContentType.PDF,
                    filePath = stored.absolutePath,
                    previewText = "doc",
                    timestamp = 0L,
                ),
            )
            val shared =
                File(app.cacheDir, "doc.pdf").also {
                    it.writeBytes("%PDF".toByteArray() + ByteArray(16))
                }
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shared))
                    .setType("application/pdf")
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()
            assertEquals(1, dao.getAllOnce().size)

            // Act
            viewModel.openExistingShare()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(existingId, viewModel.navigateToNewClip.value)
            assertNull(viewModel.duplicateShare.value)
            assertEquals(1, dao.getAllOnce().size)
        }

    @Test
    fun duplicateFileShare_withText_savesTextClipAndToastsSaved() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val stored =
                File(app.filesDir, "files").also {
                    it.mkdirs()
                }.resolve("blob.bin")
            stored.writeBytes(ByteArray(21) { 3 })
            repository.insertEntry(
                ClipboardEntry(
                    id = 404L,
                    contentType = ContentType.FILE,
                    filePath = stored.absolutePath,
                    previewText = "blob",
                    timestamp = 0L,
                ),
            )
            val shared =
                File(app.cacheDir, "blob.bin").also {
                    it.writeBytes(ByteArray(21) { 3 })
                }
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shared))
                    .putExtra(Intent.EXTRA_TEXT, "note about the file")
                    .setType("*/*")

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val items = dao.getAllOnce()
            assertEquals(2, items.size)
            assertTrue(items.any { it.contentType == ContentType.TEXT })
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    @Test
    fun textOnlyShare_ofSameString_stillInserts() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            repository.insertEntry(
                ClipboardEntry(
                    id = 505L,
                    contentType = ContentType.TEXT,
                    textContent = "hello share",
                    previewText = "hello share",
                    timestamp = 0L,
                ),
            )
            val intent = Intent().putExtra(Intent.EXTRA_TEXT, "hello share")

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val items = dao.getAllOnce()
            assertEquals(2, items.size)
            assertNull(viewModel.duplicateShare.value)
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    @Test
    fun missingPdfStream_failureOffersRetry() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val missing = File(app.cacheDir, "missing.pdf")
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(missing))
                    .setType("application/pdf")
            val noticeDeferred =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.toastEvents.first()
                }

            // Act
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val notice = noticeDeferred.await()
            assertEquals(string(R.string.clip_save_failed), notice.message)
            assertEquals(string(R.string.retry), notice.actionLabel)
            assertEquals(ClipNoticeAction.RetrySaveClipboard, notice.action)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun retryAfterSharedPdfFailure_savesSharedPdfInsteadOfClipboard() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val shared = File(app.cacheDir, "shared.pdf")
            val intent =
                Intent()
                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shared))
                    .setType("application/pdf")
            clipboard.setPrimaryClip(ClipData.newPlainText("clip", "clipboard text"))
            viewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()
            assertTrue(dao.getAllOnce().isEmpty())

            // Act
            shared.writeBytes("%PDF".toByteArray() + ByteArray(16))
            viewModel.retryLastSave()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = dao.getAllOnce().single()
            assertEquals(ContentType.PDF, item.contentType)
            assertEquals(string(R.string.clip_saved), viewModel.toastMessage.value)
        }

    @Test
    fun retryAfterSharedTextFailure_savesSharedTextInsteadOfClipboard() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val failingDao = FailingInsertDao(FakeClipboardItemDao(), failuresRemaining = 1)
            val failingViewModel = newViewModel(failingDao)
            clipboard.setPrimaryClip(ClipData.newPlainText("clip", "clipboard text"))
            val intent = Intent().putExtra(Intent.EXTRA_TEXT, "shared text")
            val noticeDeferred =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    failingViewModel.toastEvents.first()
                }
            failingViewModel.saveSharedContent(intent)
            mainDispatcherRule.scheduler.advanceUntilIdle()
            val notice = noticeDeferred.await()
            assertEquals(string(R.string.clip_save_failed), notice.message)
            assertEquals(string(R.string.retry), notice.actionLabel)
            assertEquals(ClipNoticeAction.RetrySaveClipboard, notice.action)

            // Act
            failingViewModel.retryLastSave()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val item = failingDao.getAllOnce().single()
            assertEquals(ContentType.TEXT, item.contentType)
            assertEquals("shared text", item.textContent)
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
