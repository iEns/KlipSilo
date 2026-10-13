package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SaveAnnotatedPdfTest {
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
        File(app.filesDir, "pdfs").deleteRecursively()
    }

    @Test
    fun saveAnnotatedPdf_missingFile_toastsFailure_andDoesNotNavigate() =
        runTest(mainDispatcherRule.dispatcher) {
            val missing = File(app.filesDir, "pdfs/missing-annotated.pdf")
            val entry = pdfEntry(missing, id = 51L)

            viewModel.saveAnnotatedPdf(entry, listOf(PdfAnnotation(0, 0.2f, 0.2f, "Note", 0.02f)))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            assertFalse(viewModel.navigateBack.value)
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun saveAnnotatedPdf_success_toastsSaved_andNavigatesBack() =
        runTest(mainDispatcherRule.dispatcher) {
            val file = writeBlankPdf("annotate-nav.pdf")
            val entry = pdfEntry(file, id = 52L)

            viewModel.saveAnnotatedPdf(entry, listOf(PdfAnnotation(0, 0.2f, 0.2f, "Note", 0.02f)))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            assertTrue(viewModel.navigateBack.value)
            assertEquals(string(R.string.pdf_saved), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    private fun pdfEntry(
        file: File,
        id: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.PDF,
        filePath = file.absolutePath,
        previewText = file.nameWithoutExtension,
        timestamp = 1L,
    )

    private fun string(resId: Int): String = LocaleHelper.applyLocale(app).getString(resId)

    private fun writeBlankPdf(name: String): File {
        val dir = File(app.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.A4))
            document.save(file)
        }
        return file
    }
}
