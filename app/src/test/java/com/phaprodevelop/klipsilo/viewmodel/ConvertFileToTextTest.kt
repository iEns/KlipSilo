package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
class ConvertFileToTextTest {
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
        File(app.filesDir, "images").deleteRecursively()
        File(app.filesDir, "pdfs").deleteRecursively()
    }

    @Test
    fun convertFileToText_txtHello_createsTextClip() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val file = storedFile("hello.txt", "hello".toByteArray())
            val entry = fileEntry(file, id = 11L)

            // Act
            viewModel.convertFileToText(entry)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val textItem = dao.getAllOnce().single { it.contentType == ContentType.TEXT }
            assertEquals("hello", textItem.textContent)
            assertNull(textItem.filePath)
            assertEquals(textItem.id, viewModel.navigateToNewClip.value)
            assertEquals(string(R.string.file_to_text), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertFileToText_nullPath_toastsFailed() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val entry =
                ClipboardEntry(
                    id = 12L,
                    contentType = ContentType.FILE,
                    filePath = null,
                    previewText = "none",
                    timestamp = 1L,
                )

            // Act
            viewModel.convertFileToText(entry)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertTrue(dao.getAllOnce().none { it.contentType == ContentType.TEXT })
            assertNull(viewModel.navigateToNewClip.value)
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertFileToText_binWithNul_toastsFailed() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val file = storedFile("sample.bin", byteArrayOf(0x00, 0x01, 0x02, 'A'.code.toByte()))
            val entry = fileEntry(file, id = 13L)

            // Act
            viewModel.convertFileToText(entry)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertTrue(dao.getAllOnce().none { it.contentType == ContentType.TEXT })
            assertNull(viewModel.navigateToNewClip.value)
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertFileToText_nullPathOnListRow_usesGetClip() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val file = storedFile("from-db.txt", "from-db".toByteArray())
            val stored = fileEntry(file, id = 14L)
            ClipboardRepository(dao, app).insertEntry(stored)
            val listRow = stored.copy(filePath = null)

            // Act
            viewModel.convertFileToText(listRow)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val textItem = dao.getAllOnce().single { it.contentType == ContentType.TEXT }
            assertEquals("from-db", textItem.textContent)
            assertEquals(string(R.string.file_to_text), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertTextToFile_blankText_readsStoredBody() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val stored =
                ClipboardEntry(
                    id = 21L,
                    contentType = ContentType.TEXT,
                    textContent = "stored body",
                    filePath = null,
                    previewText = "stored body",
                    timestamp = 1L,
                )
            ClipboardRepository(dao, app).insertEntry(stored)
            val listRow = stored.copy(textContent = null)

            // Act
            viewModel.convertTextToFile("", listRow)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            val fileItem = dao.getAllOnce().single { it.contentType == ContentType.FILE }
            assertNotNull(fileItem.filePath)
            assertEquals("stored body", File(fileItem.filePath!!).readText())
            assertEquals(string(R.string.text_to_txt), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertTextToPdf_blankAndNoBody_toastsFailed() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val empty =
                ClipboardEntry(
                    id = 23L,
                    contentType = ContentType.TEXT,
                    textContent = null,
                    filePath = null,
                    previewText = "",
                    timestamp = 1L,
                )

            // Act
            viewModel.convertTextToPdf("", empty)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertTrue(dao.getAllOnce().none { it.contentType == ContentType.PDF })
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun convertTextToFile_doesNotRequestNavigateBack() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange (#129): Text → TXT runs from the Clips action sheet. A
            // pending "navigate back" would make the next clip tap open a
            // detail screen and immediately close it again.
            val stored =
                ClipboardEntry(
                    id = 24L,
                    contentType = ContentType.TEXT,
                    textContent = "body",
                    filePath = null,
                    previewText = "body",
                    timestamp = 1L,
                )
            ClipboardRepository(dao, app).insertEntry(stored)

            // Act
            viewModel.convertTextToFile("body", stored)
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertNotNull(dao.getAllOnce().single { it.contentType == ContentType.FILE }.filePath)
            assertFalse(viewModel.navigateBack.value)
        }

    private fun storedFile(
        name: String,
        bytes: ByteArray,
    ): File {
        val dir = File(app.filesDir, "files").apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(bytes) }.also { assertTrue(it.exists()) }
    }

    private fun fileEntry(
        file: File,
        id: Long,
    ): ClipboardEntry {
        assertNotNull(file.absolutePath)
        return ClipboardEntry(
            id = id,
            contentType = ContentType.FILE,
            filePath = file.absolutePath,
            previewText = file.name,
            timestamp = 1_700_000_000_000L,
        )
    }

    private fun string(resId: Int): String = LocaleHelper.applyLocale(app).getString(resId)
}
