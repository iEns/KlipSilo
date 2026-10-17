package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Three checkbox fields that all carry the field name "Sessions" and differ only in their /AP /N
 * on-state values, like the test form from #57 where the QA list showed three identical rows.
 */
@RunWith(RobolectricTestRunner::class)
class FormFieldCheckboxGroupTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var groupPdf: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
        groupPdf = buildGroupPdf(File(context.cacheDir, "sessions_group.pdf"))
    }

    @Test
    fun kidLabels_comeFromTheOnStateExportValue() =
        runTest {
            val fields = repo.getPdfFormFields(groupPdf.absolutePath)
            assertEquals(3, fields.size)
            assertTrue(fields.all { it.fullyQualifiedName == "Sessions" })
            assertEquals(
                listOf("Morning session", "Afternoon session", "Dinner"),
                fields.map { it.label },
            )
            // One key per box, otherwise the value map would check all three at once.
            assertEquals(3, fields.map { it.valueKey }.distinct().size)
            assertTrue(fields.all { it.valueKey.startsWith("Sessions$CHECK_KID_KEY_SEPARATOR") })
        }

    @Test
    fun fill_savesANewPdf() =
        runTest {
            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.PDF,
                    filePath = groupPdf.absolutePath,
                    previewText = "Workshop",
                    timestamp = 1L,
                )
            val fields = repo.getPdfFormFields(groupPdf.absolutePath)
            val morningKey = fields.first { it.label == "Morning session" }.valueKey
            assertEquals("Sessions${CHECK_KID_KEY_SEPARATOR}Morning_session", morningKey)
            val newId = repo.fillPdfForm(groupPdf.absolutePath, entry, mapOf(morningKey to "true"))
            assertTrue(newId > 0L)
            dao.getAllOnce().first { it.id == newId }.filePath.let { File(it).delete() }
        }

    @Test
    fun applyFillValue_checksExactlyTheChosenBox() {
        PDDocument.load(groupPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            val boxes = acroForm.fields.filterIsInstance<PDCheckBox>()
            assertEquals(3, boxes.size)
            assertTrue(
                repo.applyFillValue(
                    acroForm,
                    "Sessions$CHECK_KID_KEY_SEPARATOR${"Morning_session"}",
                    "true",
                ),
            )
            assertEquals("Morning_session", boxes[0].valueAsString)
            assertTrue(boxes[0].widgets.first().appearanceState.getName() != "Off")
            // The other two boxes of the group stay untouched.
            assertEquals("Off", boxes[1].widgets.first().appearanceState.getName())
            assertEquals("Off", boxes[2].widgets.first().appearanceState.getName())
            assertTrue(boxes[1].valueAsString != "Morning_session")
            // Unchecking does the reverse.
            assertTrue(
                repo.applyFillValue(
                    acroForm,
                    "Sessions$CHECK_KID_KEY_SEPARATOR${"Morning_session"}",
                    "false",
                ),
            )
            assertEquals("Off", boxes[0].widgets.first().appearanceState.getName())
        }
    }

    /** A "Sessions" group of three same-named checkbox widgets with page text to their right. */
    private fun buildGroupPdf(outFile: File): File {
        val options = listOf("Morning_session", "Afternoon_session", "Dinner")
        val document = PDDocument()
        val page = PDPage(PDRectangle(612f, 792f))
        document.addPage(page)
        val acroForm = PDAcroForm(document)
        document.documentCatalog.acroForm = acroForm
        val boxes =
            options.mapIndexed { index, option ->
                val box = PDCheckBox(acroForm)
                box.partialName = "Sessions"
                val boxDictionary = box.cosObject
                boxDictionary.setItem(COSName.SUBTYPE, COSName.WIDGET)
                val bottom = 710f - index * 24f
                boxDictionary.setItem(COSName.RECT, PDRectangle(90f, bottom, 20f, 12f))
                boxDictionary.setItem(COSName.P, page)
                val normalStates = COSDictionary()
                normalStates.setItem(
                    COSName.getPDFName("Off"),
                    PDStream(document, ByteArrayInputStream(byteArrayOf())).cosObject,
                )
                normalStates.setItem(
                    COSName.getPDFName(option),
                    PDStream(document, ByteArrayInputStream(byteArrayOf())).cosObject,
                )
                boxDictionary.setItem(COSName.AP, COSDictionary().also { it.setItem(COSName.N, normalStates) })
                boxDictionary.setItem(COSName.AS, COSName.getPDFName("Off"))
                box
            }
        acroForm.fields = boxes
        PDPageContentStream(document, page).use { stream ->
            stream.setFont(PDType1Font.HELVETICA, 10f)
            options.forEachIndexed { index, option ->
                stream.beginText()
                stream.newLineAtOffset(128f, 712f - index * 24f)
                stream.showText(option.replace('_', ' '))
                stream.endText()
            }
        }
        val bytes = java.io.ByteArrayOutputStream().also { document.save(it) }.toByteArray()
        document.close()
        outFile.writeBytes(bytes)
        return outFile
    }
}
