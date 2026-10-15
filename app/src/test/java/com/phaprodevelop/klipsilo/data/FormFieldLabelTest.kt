@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import org.junit.Assert.assertEquals
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
