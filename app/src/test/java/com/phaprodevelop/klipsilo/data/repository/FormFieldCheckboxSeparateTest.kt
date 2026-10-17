package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.common.PDStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Three SEPARATE checkbox fields (s0/s1/s2, no shared name, no /TU, on-state /Yes) with the
 * option text printed to the right of each box and a "Sessions" heading above, exactly like the
 * QA sample from #57 (Event_Registration_Form.pdf). The rows must not all read "Sessions".
 */
@RunWith(RobolectricTestRunner::class)
class FormFieldCheckboxSeparateTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var separatePdf: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        repo = ClipboardRepository(FakeClipboardItemDao(), context)
        separatePdf = buildSeparatePdf(File(context.cacheDir, "sessions_separate.pdf"))
    }

    @Test
    fun labels_comeFromThePageTextRightOfEachWidget() =
        runTest {
            val fields = repo.getPdfFormFields(separatePdf.absolutePath)
            assertEquals(listOf("s0", "s1", "s2"), fields.map { it.name })
            assertEquals(
                listOf("Morning session", "Afternoon session", "Dinner"),
                fields.map { it.label },
            )
            // Separate fields: the ordinary per-field key applies.
            assertEquals(listOf("s0", "s1", "s2"), fields.map { it.valueKey })
        }

    /** Like the #57 sample: cryptic names, /Yes on-state, no tooltips, labels to the right. */
    private fun buildSeparatePdf(outFile: File): File {
        val options = listOf("Morning session", "Afternoon session", "Dinner")
        val document = PDDocument()
        val page = PDPage(PDRectangle(612f, 792f))
        document.addPage(page)
        val acroForm = PDAcroForm(document)
        document.documentCatalog.acroForm = acroForm
        val boxes =
            options.mapIndexed { index, _ ->
                val box = PDCheckBox(acroForm)
                box.partialName = "s$index"
                val boxDictionary = box.cosObject
                boxDictionary.setItem(COSName.SUBTYPE, COSName.WIDGET)
                val bottom = 700f - index * 24f
                boxDictionary.setItem(COSName.RECT, PDRectangle(90f, bottom, 20f, 12f))
                boxDictionary.setItem(COSName.P, page)
                val normalStates = COSDictionary()
                normalStates.setItem(
                    COSName.getPDFName("Off"),
                    PDStream(document, ByteArrayInputStream(byteArrayOf())).cosObject,
                )
                normalStates.setItem(
                    COSName.getPDFName("Yes"),
                    PDStream(document, ByteArrayInputStream(byteArrayOf())).cosObject,
                )
                boxDictionary.setItem(COSName.AP, COSDictionary().also { it.setItem(COSName.N, normalStates) })
                boxDictionary.setItem(COSName.AS, COSName.getPDFName("Off"))
                box
            }
        acroForm.fields = boxes
        PDPageContentStream(document, page).use { stream ->
            val font = PDType1Font.HELVETICA
            stream.setFont(font, 10f)
            stream.beginText()
            stream.newLineAtOffset(90f, 730f)
            stream.showText("Sessions")
            stream.endText()
            options.forEachIndexed { index, option ->
                stream.beginText()
                stream.newLineAtOffset(128f, 702f - index * 24f)
                stream.showText(option)
                stream.endText()
            }
        }
        val bytes = ByteArrayOutputStream().also { document.save(it) }.toByteArray()
        document.close()
        outFile.writeBytes(bytes)
        return outFile
    }
}
