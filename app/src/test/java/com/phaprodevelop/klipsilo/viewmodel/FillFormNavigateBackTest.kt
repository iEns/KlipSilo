@file:Suppress("ktlint:standard:function-naming")

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
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FillFormNavigateBackTest {
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
    fun fillForm_missingFile_doesNotNavigateBack() =
        runTest(mainDispatcherRule.dispatcher) {
            val missing = File(app.filesDir, "pdfs/missing-form.pdf")
            val entry = pdfEntry(missing, id = 31L)

            viewModel.fillForm(entry, mapOf("Name" to "Ada"))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            assertFalse(viewModel.navigateBack.value)
            assertEquals(string(R.string.clip_save_failed), viewModel.toastMessage.value)
            assertFalse(viewModel.isConverting.value)
        }

    @Test
    fun fillForm_success_setsNavigateBack() =
        runTest(mainDispatcherRule.dispatcher) {
            val formFile = writeAcroFormPdf("form-nav.pdf")
            assumeTrue(
                "PDFBox AcroForm creation is not feasible under this Robolectric environment",
                formFile != null,
            )
            val entry = pdfEntry(formFile!!, id = 32L)
            ClipboardRepository(dao, app).insertEntry(entry)

            viewModel.fillForm(entry, mapOf("Name" to "Ada"))
            mainDispatcherRule.scheduler.advanceUntilIdle()

            assertTrue(viewModel.navigateBack.value)
            assertEquals(string(R.string.form_filled), viewModel.toastMessage.value)
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

    private fun writeAcroFormPdf(name: String): File? {
        return try {
            val dir = File(app.filesDir, "pdfs").apply { mkdirs() }
            val file = File(dir, name)
            PDDocument().use { document ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                val acroForm = PDAcroForm(document)
                val resources = PDResources()
                resources.put(COSName.HELV, PDType1Font.HELVETICA)
                acroForm.defaultResources = resources
                acroForm.defaultAppearance = "/Helv 12 Tf 0 g"
                document.documentCatalog.acroForm = acroForm
                val textField = PDTextField(acroForm)
                textField.partialName = "Name"
                textField.defaultAppearance = "/Helv 12 Tf 0 g"
                val textWidget = textField.widgets.first()
                textWidget.rectangle = PDRectangle(100f, 700f, 200f, 20f)
                textWidget.page = page
                textWidget.isPrinted = true
                val checkBox = PDCheckBox(acroForm)
                checkBox.partialName = "Agree"
                val checkWidget = checkBox.widgets.first()
                checkWidget.rectangle = PDRectangle(100f, 660f, 20f, 20f)
                checkWidget.page = page
                checkWidget.isPrinted = true
                val onStream = PDAppearanceStream(document).apply { bBox = PDRectangle(20f, 20f) }
                val offStream = PDAppearanceStream(document).apply { bBox = PDRectangle(20f, 20f) }
                val states = COSDictionary()
                states.setItem(COSName.YES, onStream)
                states.setItem(COSName.Off, offStream)
                checkWidget.appearance =
                    PDAppearanceDictionary().apply {
                        setNormalAppearance(PDAppearanceEntry(states))
                    }
                acroForm.fields = listOf(textField, checkBox)
                page.annotations = listOf<PDAnnotation>(textWidget, checkWidget)
                document.save(file)
            }
            val loaded =
                PDDocument.load(file).use { document ->
                    document.documentCatalog.acroForm?.fields.orEmpty().mapNotNull { it.partialName }
                }
            if (loaded.contains("Name")) file else null
        } catch (_: Exception) {
            null
        }
    }
}
