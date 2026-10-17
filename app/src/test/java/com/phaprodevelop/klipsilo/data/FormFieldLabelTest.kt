@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormFieldLabelTest {
    @Test
    fun tooltip_winsOverPartialName() {
        val label =
            resolveFormFieldLabel(
                partialName = "name",
                alternateName = "Full name",
                type = FormFieldType.TEXT,
                nearbyText = "Ignored",
                fallbackNumber = 1,
            )
        assertEquals(ResolvedFieldLabel.Literal("Full name"), label)
    }

    @Test
    fun readableName_isPrettified() {
        assertEquals("Email", literal("email", FormFieldType.TEXT))
        assertEquals("First name", literal("first_name", FormFieldType.TEXT))
        assertEquals("First name", literal("firstName", FormFieldType.TEXT))
    }

    @Test
    fun crypticName_usesNearbyText() {
        val label =
            resolveFormFieldLabel(
                partialName = "s0",
                alternateName = null,
                type = FormFieldType.CHECKBOX,
                nearbyText = "I agree",
                fallbackNumber = 4,
            )
        assertEquals(ResolvedFieldLabel.Literal("I agree"), label)
    }

    @Test
    fun crypticName_withoutNearby_isNumbered() {
        val label =
            resolveFormFieldLabel(
                partialName = "Check Box1",
                alternateName = "  ",
                type = FormFieldType.CHECKBOX,
                nearbyText = null,
                fallbackNumber = 2,
            )
        assertEquals(ResolvedFieldLabel.Numbered(FormFieldType.CHECKBOX, 2), label)
    }

    @Test
    fun nearbyText_prefersTheLabelOnTheLeft() {
        val widget = PdfUserRect(left = 100f, bottom = 700f, right = 300f, top = 720f)
        val text =
            nearbyFieldText(
                widget,
                listOf(
                    PdfTextRun("Phone", left = 40f, baseline = 706f, width = 40f, height = 12f),
                    PdfTextRun("Other", left = 100f, baseline = 640f, width = 40f, height = 12f),
                ),
            )
        assertEquals("Phone", text)
    }

    @Test
    fun nearbyText_joinsWordsOnTheSameLine() {
        val widget = PdfUserRect(left = 120f, bottom = 700f, right = 300f, top = 720f)
        val text =
            nearbyFieldText(
                widget,
                listOf(
                    PdfTextRun("Full", left = 40f, baseline = 706f, width = 24f, height = 12f),
                    PdfTextRun("name", left = 68f, baseline = 706f, width = 30f, height = 12f),
                ),
            )
        assertEquals("Full name", text)
    }

    @Test
    fun nearbyText_usesTheLineAboveWhenNothingIsOnTheLeft() {
        val widget = PdfUserRect(left = 100f, bottom = 700f, right = 200f, top = 720f)
        val text =
            nearbyFieldText(
                widget,
                listOf(PdfTextRun("Email", left = 100f, baseline = 736f, width = 40f, height = 12f)),
            )
        assertEquals("Email", text)
    }

    @Test
    fun nearbyText_farAway_isIgnored() {
        val widget = PdfUserRect(left = 100f, bottom = 700f, right = 200f, top = 720f)
        val text =
            nearbyFieldText(
                widget,
                listOf(PdfTextRun("Footer", left = 100f, baseline = 40f, width = 40f, height = 12f)),
            )
        assertNull(text)
    }

    @Test
    fun visualRect_unrotated_measuresFromTheTop() {
        val visual =
            widgetVisualFractions(
                widget = PdfUserRect(100f, 700f, 300f, 720f),
                pageLeft = 0f,
                pageBottom = 0f,
                pageWidth = 595f,
                pageHeight = 842f,
                rotation = 0,
            )
        assertEquals(100f / 595f, visual!!.left, 0.001f)
        assertEquals(300f / 595f, visual.right, 0.001f)
        assertEquals((842f - 720f) / 842f, visual.top, 0.001f)
        assertEquals((842f - 700f) / 842f, visual.bottom, 0.001f)
        assertTrue(visual.top < visual.bottom)
    }

    @Test
    fun visualRect_rotation90_movesBottomLeftTowardTheTop() {
        val visual =
            widgetVisualFractions(
                widget = PdfUserRect(0f, 0f, 20f, 20f),
                pageLeft = 0f,
                pageBottom = 0f,
                pageWidth = 200f,
                pageHeight = 400f,
                rotation = 90,
            )
        assertEquals(0f, visual!!.left, 0.001f)
        assertEquals(0f, visual.top, 0.001f)
        assertEquals(20f / 400f, visual.right, 0.001f)
        assertEquals(20f / 200f, visual.bottom, 0.001f)
    }

    @Test
    fun textFlags_parseBits13_14_and_29() {
        val empty = parseTextFieldFlags(0L)
        assertFalse(empty.multiline)
        assertFalse(empty.password)
        assertFalse(empty.comb)
        val none = parseTextFieldFlags(null)
        assertFalse(none.multiline)
        assertFalse(none.password)
        assertFalse(none.comb)
        // Observed in the wild: exactly the multiline flag (#52).
        assertEquals(
            TextFieldFlagBits(multiline = true, password = false, comb = false),
            parseTextFieldFlags(TEXT_FIELD_FLAG_MULTILINE),
        )
        assertTrue(parseTextFieldFlags(TEXT_FIELD_FLAG_PASSWORD).password)
        assertTrue(parseTextFieldFlags(TEXT_FIELD_FLAG_COMB).comb)
        assertTrue(
            parseTextFieldFlags(
                TEXT_FIELD_FLAG_MULTILINE or TEXT_FIELD_FLAG_PASSWORD or 2L or 4L,
            ).multiline,
        )
    }

    @Test
    fun keyboardKind_emailWords() {
        assertEquals(FormKeyboardKind.EMAIL, formKeyboardKind("Email", null, null))
        assertEquals(FormKeyboardKind.EMAIL, formKeyboardKind("e_mail_address", null, null))
        assertEquals(FormKeyboardKind.EMAIL, formKeyboardKind(null, "Kontakt", "E-Mail"))
        assertEquals(FormKeyboardKind.EMAIL, formKeyboardKind("fld_7", null, "Mail address"))
        assertEquals(FormKeyboardKind.EMAIL, formKeyboardKind("UserEmail", null, null))
    }

    @Test
    fun keyboardKind_phoneWords() {
        assertEquals(FormKeyboardKind.PHONE, formKeyboardKind("Phone", null, null))
        assertEquals(FormKeyboardKind.PHONE, formKeyboardKind("Tel.", null, null))
        assertEquals(FormKeyboardKind.PHONE, formKeyboardKind(null, "Telefon", null))
        assertEquals(FormKeyboardKind.PHONE, formKeyboardKind("Mobil", null, null))
        // A field named "Phone number" is phone, not number.
        assertEquals(FormKeyboardKind.PHONE, formKeyboardKind("Phone number", null, null))
    }

    @Test
    fun keyboardKind_numberWords() {
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("Number", null, null))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("ZIP", null, null))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("Postnr", null, null))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind(null, null, "Postcode"))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("Age", null, null))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("Alder", null, null))
        assertEquals(FormKeyboardKind.NUMBER, formKeyboardKind("Year", null, null))
    }

    @Test
    fun keyboardKind_otherNames_stayText() {
        assertEquals(FormKeyboardKind.TEXT, formKeyboardKind("Name", null, null))
        assertEquals(FormKeyboardKind.TEXT, formKeyboardKind("Date", null, null))
        assertEquals(FormKeyboardKind.TEXT, formKeyboardKind("Fødselsdato", null, null))
        // Keyword only as part of an unrelated word must not match.
        assertEquals(FormKeyboardKind.TEXT, formKeyboardKind("package_count", null, null))
        assertEquals(FormKeyboardKind.TEXT, formKeyboardKind("Telephone_holder", null, "microphone note"))
    }

    private fun literal(
        name: String,
        type: FormFieldType,
    ): String {
        val resolved =
            resolveFormFieldLabel(
                partialName = name,
                alternateName = null,
                type = type,
                nearbyText = null,
                fallbackNumber = 1,
            )
        return (resolved as ResolvedFieldLabel.Literal).text
    }
}
