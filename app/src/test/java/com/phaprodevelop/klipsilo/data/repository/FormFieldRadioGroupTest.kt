package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.CHECKBOX
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.RADIO
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The two public testaroo.com sample forms (see app/src/test/resources/forms/README.md):
 * radio groups there are one parent /Btn field with several widget kids and per-kid
 * /AP /N on-states but no /Ff Radio flag, so every kid must become a radio option and
 * the save must write /V on the parent and /AS on every kid (#67).
 */
@RunWith(RobolectricTestRunner::class)
class FormFieldRadioGroupTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var jobPdf: File
    private lateinit var eventPdf: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
        jobPdf = fixture("forms/job-application.pdf")
        eventPdf = fixture("forms/event-registration.pdf")
    }

    @Test
    fun jobApplication_radioGroupHasOneRowPerKidOption() =
        runTest {
            val fields = repo.getPdfFormFields(jobPdf.absolutePath)
            val auth = fields.filter { it.fullyQualifiedName == "auth" }
            assertEquals(1, auth.size)
            assertEquals(RADIO, auth.single().type)
            // The three widget kids, in page order, each with its own export value.
            assertEquals(listOf("citizen", "visa", "sponsor"), auth.single().options)
            // The printed option text is what the fill screen shows; export values keep saving.
            assertEquals(
                listOf("Citizen / Permanent resident", "Visa holder", "Needs sponsorship"),
                auth.single().optionLabels,
            )
            // The group header is the printed heading, never the first option text or /TU (#78).
            assertEquals("Work authorization", auth.single().label)
            assertEquals(listOf("citizen", "visa", "sponsor"), auth.single().hits.map { it.option })
            assertEquals("auth", auth.single().valueKey)
            assertEquals("citizen", auth.single().currentValue)
        }

    @Test
    fun eventRegistration_radioGroupAndPlainCheckboxesStayApart() =
        runTest {
            val fields = repo.getPdfFormFields(eventPdf.absolutePath)
            val shirt = fields.filter { it.fullyQualifiedName == "shirt" }
            assertEquals(1, shirt.size)
            assertEquals(RADIO, shirt.single().type)
            assertEquals(listOf("s", "m", "l", "xl"), shirt.single().options)
            assertEquals(listOf("S", "M", "L", "XL"), shirt.single().optionLabels)
            assertEquals("T-shirt size", shirt.single().label)
            assertEquals("m", shirt.single().currentValue)
            // Same-name session checkboxes are separate fields, not a radio group.
            assertEquals(
                listOf("s_keynote", "s_work", "s_panel"),
                fields.filter { it.type == CHECKBOX }.map { it.fullyQualifiedName },
            )
        }

    @Test
    fun save_writesVOnTheParentAndASOnEveryKid() {
        PDDocument.load(jobPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            assertTrue(repo.applyFillValue(acroForm, "auth", "visa"))
            val field = acroForm.getField("auth")
            assertEquals("visa", field.valueAsString)
            val kids = field.widgets
            assertEquals(3, kids.size)
            // /AS on every kid: the chosen one gets /visa, the others /Off.
            assertEquals("Off", kids[0].appearanceState.getName())
            assertEquals("visa", kids[1].appearanceState.getName())
            assertEquals("Off", kids[2].appearanceState.getName())
        }
    }

    @Test
    fun save_fourKidRadioGroupPicksTheRightOne() {
        PDDocument.load(eventPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            assertTrue(repo.applyFillValue(acroForm, "shirt", "l"))
            val kids = acroForm.getField("shirt").widgets
            assertEquals(4, kids.size)
            assertEquals(listOf("Off", "Off", "l", "Off"), kids.map { it.appearanceState.getName() })
        }
    }

    @Test
    fun save_untouchedRadioRowLeavesTheFieldAlone() {
        PDDocument.load(jobPdf).use { document ->
            val acroForm = document.documentCatalog.acroForm!!
            assertTrue(repo.applyFillValue(acroForm, "auth", "Off"))
            assertEquals("citizen", acroForm.getField("auth").valueAsString)
            assertEquals(
                listOf("citizen", "Off", "Off"),
                acroForm.getField("auth").widgets.map { it.appearanceState.getName() },
            )
        }
    }

    @Test
    fun save_roundTripProducesAReopenablePdf() =
        runTest {
            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.PDF,
                    filePath = jobPdf.absolutePath,
                    previewText = "Job application",
                    timestamp = 1L,
                )
            val newId = repo.fillPdfForm(jobPdf.absolutePath, entry, mapOf("auth" to "sponsor"))
            assertTrue(newId > 0L)
            dao.getAllOnce().first { it.id == newId }.filePath.let { File(it).delete() }
        }

    private fun fixture(path: String): File =
        File(
            javaClass.classLoader
                .getResource(path)!!
                .toURI(),
        )
}
