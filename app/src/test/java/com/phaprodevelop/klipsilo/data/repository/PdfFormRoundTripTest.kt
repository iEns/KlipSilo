@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Form fill read → write → reread round-trip (#80): getPdfFormFields reports
 * the field rows, fillFormCopy writes a text, a choice/radio and a checkbox
 * value to a copy, and the copy reads those values back. The source fixture
 * itself must stay byte-for-byte unchanged.
 */
@RunWith(RobolectricTestRunner::class)
class PdfFormRoundTripTest {
    private lateinit var app: Application
    private lateinit var repo: ClipboardRepository
    private lateinit var formPdf: File
    private lateinit var filled: File

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repo = ClipboardRepository(FakeClipboardItemDao(), app)
        formPdf = fixture("forms/event-registration.pdf")
        filled = File(app.cacheDir, "form-roundtrip-${System.nanoTime()}.pdf")
    }

    @After
    fun tearDown() {
        if (::filled.isInitialized) filled.delete()
        if (::formPdf.isInitialized) formPdf.delete()
    }

    @Test
    fun fillFormCopy_textChoiceAndCheckbox_surviveReread_andSourceIsUnchanged() =
        runTest {
            // Arrange
            val sourceBytes = formPdf.readBytes()
            val before = repo.getPdfFormFields(formPdf.absolutePath)
            val textField = before.first { it.type == FormFieldType.TEXT }
            val choiceField = before.first { it.type == FormFieldType.CHOICE }
            val checkboxField = before.first { it.type == FormFieldType.CHECKBOX }
            val chosenOption = choiceField.options.first()
            val values =
                mapOf(
                    textField.valueKey to "Round-trip text",
                    choiceField.valueKey to chosenOption,
                    checkboxField.valueKey to "true",
                )

            // Act
            assertTrue(repo.fillFormCopy(formPdf.absolutePath, values, filled))
            val after = repo.getPdfFormFields(filled.absolutePath)

            // Assert
            val textAfter = after.single { it.valueKey == textField.valueKey }
            val choiceAfter = after.single { it.valueKey == choiceField.valueKey }
            val checkboxAfter = after.single { it.valueKey == checkboxField.valueKey }
            assertEquals(FormFieldType.TEXT, textAfter.type)
            assertEquals("Round-trip text", textAfter.currentValue)
            assertEquals(FormFieldType.CHOICE, choiceAfter.type)
            assertEquals(chosenOption, choiceAfter.currentValue)
            assertEquals(FormFieldType.CHECKBOX, checkboxAfter.type)
            assertNotEquals("Off", checkboxAfter.currentValue)
            assertNotEquals("false", checkboxAfter.currentValue)
            assertArrayEquals("the source fixture must not be modified", sourceBytes, formPdf.readBytes())
        }

    private fun fixture(path: String): File {
        val stream = javaClass.classLoader!!.getResourceAsStream(path)!!
        val file = File.createTempFile("event-registration", ".pdf")
        stream.use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
