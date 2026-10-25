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

    @Test
    fun checkboxWidget_theNeighbourTextWinsOverEverything() {
        // Printed text right of the widget names the option (#57): even a tooltip and a
        // meaningful export value lose against it.
        assertEquals(
            ResolvedFieldLabel.Literal("Dinner"),
            resolveWidgetLabel(
                nearbyRight = "Dinner",
                tooltip = "Join the field trip",
                onState = "Morning_session",
                partialName = "s2",
                group = null,
                type = FormFieldType.CHECKBOX,
                fallbackNumber = 1,
            ),
        )
    }

    @Test
    fun checkboxWidget_tooltipWinsWhenThereIsNoNeighbourText() {
        assertEquals(
            ResolvedFieldLabel.Literal("Join the field trip"),
            resolveWidgetLabel(
                nearbyRight = null,
                tooltip = "Join the field trip",
                onState = "Morning_session",
                partialName = "s0",
                group = null,
                type = FormFieldType.CHECKBOX,
                fallbackNumber = 1,
            ),
        )
    }

    @Test
    fun checkboxWidget_meaningfulExportValue_isPrettified() {
        // "Morning_session" becomes "Morning session" (matches #57's row expectations).
        assertEquals(
            ResolvedFieldLabel.Literal("Afternoon session"),
            resolveWidgetLabel(
                nearbyRight = null,
                tooltip = null,
                onState = "Afternoon_session",
                partialName = "Sessions",
                group = "Sessions",
                type = FormFieldType.CHECKBOX,
                fallbackNumber = 1,
            ),
        )
    }

    @Test
    fun checkboxWidget_ambiguousExportValue_fallsToTheGroup() {
        // "Yes" alone is ambiguous: keep the group in front so the user knows which box.
        assertEquals(
            ResolvedFieldLabel.Literal("Sessions: Yes"),
            resolveWidgetLabel(null, null, "Yes", "Sessions", "Sessions", FormFieldType.CHECKBOX, 1),
        )
        assertTrue(isAmbiguousCheckOption("Yes"))
        assertTrue(isAmbiguousCheckOption("1"))
        assertFalse(isAmbiguousCheckOption("Morning_session"))
    }

    @Test
    fun checkboxWidget_readableName_isPrettified() {
        assertEquals(
            ResolvedFieldLabel.Literal("Newsletter"),
            resolveWidgetLabel(null, null, null, "Newsletter", null, FormFieldType.CHECKBOX, 1),
        )
    }

    @Test
    fun checkboxWidget_crypticNameWithoutHint_isNumbered() {
        // "s0" carries no meaning; the ordinary numbered fallback applies.
        assertEquals(
            ResolvedFieldLabel.Numbered(FormFieldType.CHECKBOX, 3),
            resolveWidgetLabel(null, null, null, "s0", null, FormFieldType.CHECKBOX, 3),
        )
    }

    @Test
    fun nearbyTextRight_prefersTheLabelOnTheRight() {
        val widget = PdfUserRect(left = 300f, bottom = 700f, right = 320f, top = 720f)
        val text =
            nearbyFieldTextRight(
                widget,
                listOf(
                    PdfTextRun("Morning session", left = 330f, baseline = 706f, width = 90f, height = 12f),
                    PdfTextRun("Ignored", left = 60f, baseline = 706f, width = 40f, height = 12f),
                ),
            )
        assertEquals("Morning session", text)
    }

    @Test
    fun nearbyTextRight_farAway_isIgnored() {
        val widget = PdfUserRect(left = 300f, bottom = 700f, right = 320f, top = 720f)
        val text =
            nearbyFieldTextRight(
                widget,
                listOf(PdfTextRun("Footer", left = 560f, baseline = 706f, width = 40f, height = 12f)),
            )
        assertNull(text)
    }

    @Test
    fun nearbyTextRight_stopsAtTheNextWidget() {
        // Two checkboxes in one row, each with its own option text (#57): the first label must
        // not pull in the second one's text.
        val widget = PdfUserRect(left = 300f, bottom = 700f, right = 320f, top = 720f)
        val nextWidget = PdfUserRect(left = 430f, bottom = 700f, right = 450f, top = 720f)
        val text =
            nearbyFieldTextRight(
                widget,
                listOf(
                    PdfTextRun("One", left = 330f, baseline = 706f, width = 30f, height = 12f),
                    PdfTextRun("Two", left = 360f, baseline = 706f, width = 30f, height = 12f),
                    PdfTextRun("Other option", left = 460f, baseline = 706f, width = 90f, height = 12f),
                ),
                otherWidgets = listOf(nextWidget),
            )
        assertEquals("One Two", text)
    }

    @Test
    fun nearbyTextRight_ignoresWidgetsOnAnotherLine() {
        // The next checkbox sits on the row below: it must not cut the first label short.
        val widget = PdfUserRect(left = 300f, bottom = 700f, right = 320f, top = 720f)
        val text =
            nearbyFieldTextRight(
                widget,
                listOf(
                    PdfTextRun("Full option name", left = 330f, baseline = 706f, width = 130f, height = 12f),
                ),
                otherWidgets = listOf(PdfUserRect(left = 430f, bottom = 660f, right = 450f, top = 672f)),
            )
        assertEquals("Full option name", text)
    }

    @Test
    fun radioGroupHeader_prefersPrintedTextAboveAndStripsRequiredMarker() {
        assertEquals(
            "Work authorization",
            resolveRadioGroupHeader(
                partialName = "auth",
                fieldTooltip = "Citizen/PR",
                nearbyAbove = "Work authorization *",
                firstOptionLabel = "Citizen / Permanent resident",
                firstOptionTip = "Citizen/PR",
            ),
        )
        assertEquals(
            "T-shirt size",
            resolveRadioGroupHeader(
                partialName = "shirt",
                fieldTooltip = "S",
                nearbyAbove = "T-shirt size",
                firstOptionLabel = "S",
                firstOptionTip = "S",
            ),
        )
    }

    @Test
    fun radioGroupHeader_neverUsesTheFirstOptionTextOrTip() {
        // Without page text above, an option label or option tip is not a group name (#78).
        assertNull(
            resolveRadioGroupHeader(
                partialName = "auth",
                fieldTooltip = "Citizen/PR",
                nearbyAbove = null,
                firstOptionLabel = "Citizen / Permanent resident",
                firstOptionTip = "Citizen/PR",
            ),
        )
        assertNull(
            resolveRadioGroupHeader(
                partialName = "shirt",
                fieldTooltip = "S",
                nearbyAbove = null,
                firstOptionLabel = "S",
                firstOptionTip = null,
            ),
        )
    }

    @Test
    fun radioGroupHeader_withoutAnyGroupName_isEmpty() {
        // Cryptic field name, no printed heading and no tooltip: prefer no header over "Radio 1".
        assertNull(
            resolveRadioGroupHeader(
                partialName = "radio0",
                fieldTooltip = null,
                nearbyAbove = null,
                firstOptionLabel = "Yes",
                firstOptionTip = null,
            ),
        )
    }

    @Test
    fun radioGroupHeader_readablePartialName_isTheLastResort() {
        assertEquals(
            "Newsletter",
            resolveRadioGroupHeader(
                partialName = "newsletter",
                fieldTooltip = null,
                nearbyAbove = null,
                firstOptionLabel = "Yes",
                firstOptionTip = null,
            ),
        )
    }

    @Test
    fun radioOptionLabel_prefersThePrintedTextRightOfThatWidget() {
        assertEquals(
            "Visa holder",
            resolveRadioOptionLabel(nearbyRight = "Visa holder", kidTooltip = "visa", exportValue = "visa"),
        )
        assertEquals(
            "Visa",
            resolveRadioOptionLabel(nearbyRight = null, kidTooltip = "Visa", exportValue = "visa"),
        )
        // No printed text and no tooltip: the export value is prettified for display only.
        assertEquals(
            "Morning session",
            resolveRadioOptionLabel(nearbyRight = null, kidTooltip = null, exportValue = "Morning_session"),
        )
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
