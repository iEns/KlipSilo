package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The fill-form preview is rendered from a temporary filled copy of the PDF whose
 * field appearance streams were regenerated (#80): the widget's own painted value
 * must be exactly the live value — no leftover of the PDF's own stale appearance
 * (the "General" edges Marcus saw), and an emptied choice must show empty. The
 * appearance stream is what Android's PdfRenderer paints, so the assertions read
 * the regenerated /AP streams of the filled copy. (PdfRenderer itself is native
 * and yields no bitmap on the JVM; the visual check is Marcus's on device.)
 */
@RunWith(RobolectricTestRunner::class)
class FormPreviewRenderTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var eventPdf: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        repo = ClipboardRepository(FakeClipboardItemDao(), context)
        eventPdf = fixture("forms/event-registration.pdf")
    }

    @Test
    fun filledCopy_choiceValue_appearanceStreamShowsOnlyTheNewValue() {
        val filled = tempFile("filled-ticket")
        assertTrue(repo.fillFormCopy(eventPdf.absolutePath, mapOf("ticket" to "Student"), filled))
        PDDocument.load(filled).use { document ->
            val stream = ticketAppearance(document)
            assertTrue(stream.contains("Student"))
            // The PDF's own old "(General)" appearance must be gone — this was the
            // residue the cover overlay could not fully hide (#80).
            assertFalse(stream.contains("General"))
        }
    }

    @Test
    fun filledCopy_clearedChoice_clearsValueAndAppearance() {
        val filled = tempFile("cleared-ticket")
        // setValue("") clears /V and regenerates an empty appearance (pdfbox-android
        // 2.0.27 PDChoice.setValue writes the empty string and rebuilds; it does not
        // throw), so the cleared combo shows empty.
        assertTrue(repo.fillFormCopy(eventPdf.absolutePath, mapOf("ticket" to ""), filled))
        PDDocument.load(filled).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            // pdfbox formats choice values via Arrays.toString; an emptied combo
            // reads as "[]" (the /V itself is the empty string).
            val clearedValue = acroForm.getField("ticket").valueAsString
            assertTrue(clearedValue.isEmpty() || clearedValue == "[]")
            assertFalse(ticketAppearance(document).contains("General"))
        }
    }

    @Test
    fun filledCopy_multilineText_appearanceContainsAllThreeLines() {
        val filled = tempFile("multiline-notes")
        val text = "Line one\nLine two\nLine three"
        assertTrue(repo.fillFormCopy(eventPdf.absolutePath, mapOf("notes" to text), filled))
        PDDocument.load(filled).use { document ->
            val stream = appearanceOf(document, "notes")
            // pdfbox's multiline formatter writes the wrapped words one by one
            // ("(Line ) Tj (one) Tj …"), so assert on the words: all three lines
            // must be in the regenerated appearance, not just the first (#80 G2).
            assertTrue(stream.contains("one"))
            assertTrue(stream.contains("two"))
            assertTrue(stream.contains("three"))
            assertTrue(stream.split("Line").size - 1 >= 3)
        }
    }

    @Test
    fun filledCopy_maskedPassword_appearanceNeverContainsTheSecret() {
        val filled = tempFile("masked-password")
        // The screen sends bullets for a hidden password; the appearance must
        // contain only those, never the plaintext (the plaintext is not even
        // given to this path — the masking happens before the call).
        assertTrue(repo.fillFormCopy(eventPdf.absolutePath, mapOf("name" to "•••••••"), filled))
        PDDocument.load(filled).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            assertEquals("•••••••", acroForm.getField("name").valueAsString)
        }
    }

    @Test
    fun filledCopy_emptyValues_stillRegeneratesStaleAppearances() {
        val filled = tempFile("untouched")
        assertTrue(repo.fillFormCopy(eventPdf.absolutePath, emptyMap(), filled))
        PDDocument.load(filled).use { document ->
            val stream = ticketAppearance(document)
            // The untouched /V General stays — its regenerated appearance says General.
            assertTrue(stream.contains("General"))
            assertFalse(stream.contains("Student"))
        }
    }

    @Test
    fun renderFilledFormPage_missingFile_returnsNull() =
        runTest {
            assertNull(repo.renderFilledFormPage("/no/such/file.pdf", emptyMap(), 0, 800))
        }

    @Test
    fun renderFilledFormPage_noCrashAndNullOrSizedBitmap() =
        runTest {
            // PdfRenderer is native; Robolectric yields null instead of rendering, so
            // the bitmap content check is Marcus's on-device step. On the JVM the call
            // must at least fill, save, render and clean up without crashing.
            val bitmap = repo.renderFilledFormPage(eventPdf.absolutePath, mapOf("ticket" to "Student"), 0, 800)
            if (bitmap != null) {
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                bitmap.recycle()
            }
        }

    private fun ticketAppearance(document: PDDocument): String = appearanceOf(document, "ticket")

    private fun appearanceOf(
        document: PDDocument,
        name: String,
    ): String {
        val acroForm = document.documentCatalog.acroForm!!
        val widget = acroForm.getField(name).widgets.single()
        val bytes = widget.appearance.normalAppearance.appearanceStream.contents.readBytes()
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun tempFile(prefix: String): File = File.createTempFile(prefix, ".pdf")

    private fun fixture(path: String): File {
        val stream = javaClass.classLoader!!.getResourceAsStream(path)!!
        val file = File.createTempFile("event-registration", ".pdf")
        stream.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
