package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The event-registration `ticket` field (/FT /Ch, combo flag, /Opt
 * General/Student/VIP/Speaker, /V General, /AP /N painting "(General)") (#80):
 * it must be reported as ONE choice row with ONE widget hit, the preview
 * overlay shows only the selected value, and the saved, flattened PDF paints
 * only the chosen option.
 */
@RunWith(RobolectricTestRunner::class)
class FormFieldChoiceTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var eventPdf: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        repo = ClipboardRepository(FakeClipboardItemDao(), context)
        eventPdf = fixture("forms/event-registration.pdf")
    }

    @Test
    fun ticketField_reportsOneChoiceRowWithOneHit() =
        runTest {
            val fields = repo.getPdfFormFields(eventPdf.absolutePath)
            val ticket = fields.filter { it.fullyQualifiedName == "ticket" }
            assertEquals(1, ticket.size)
            val field = ticket.single()
            assertEquals(FormFieldType.CHOICE, field.type)
            assertEquals(listOf("General", "Student", "VIP", "Speaker"), field.options)
            // Exactly ONE hit — the combo has one widget, not one per option.
            assertEquals(1, field.hits.size)
            // pdfbox formats choice values via Arrays.toString; the row must see
            // the plain export value so it matches an /Opt entry (#80).
            assertEquals("General", field.currentValue)
        }

    @Test
    fun save_comboFlattensToExactlyTheChosenValue() {
        PDDocument.load(eventPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            assertTrue(repo.applyFillValue(acroForm, "ticket", "Student"))
            acroForm.flatten()
            val saved = File("/tmp/klipsilo-b7-ticket.pdf")
            saved.outputStream().use { document.save(it) }
            val text = PDFTextStripper().getText(PDDocument.load(saved))
            // The chosen value appears exactly once; neither the PDF's own old
            // "(General)" appearance nor any other option survives the flatten.
            assertEquals(1, text.split("Student").size - 1)
            assertFalse(text.contains("General"))
            assertFalse(text.contains("VIP"))
            assertFalse(text.contains("Speaker"))
        }
    }

    @Test
    fun save_untouchedChoiceKeepsOnlyItsOwnValue() {
        PDDocument.load(eventPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            acroForm.flatten()
            val text = PDFTextStripper().getText(document)
            assertFalse(text.contains("Student"))
            assertFalse(text.contains("VIP"))
            assertFalse(text.contains("Speaker"))
            // At most one value: the field's own /V General, once.
            assertTrue(text.split("General").size - 1 <= 1)
        }
    }

    private fun fixture(path: String): File {
        val stream = javaClass.classLoader!!.getResourceAsStream(path)!!
        val file = File.createTempFile("event-registration", ".pdf")
        stream.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
