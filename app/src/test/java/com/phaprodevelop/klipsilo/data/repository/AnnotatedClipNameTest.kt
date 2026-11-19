package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotatedClipNameTest {
    private val suffix = " - annotated"
    private val untitled = "(untitled)"

    @Test
    fun stripsExtensionAndAppendsSuffix() {
        assertEquals(
            "Report - annotated",
            annotatedClipName("Report.pdf", suffix, untitled),
        )
    }

    @Test
    fun doesNotAppendSuffixTwice() {
        assertEquals(
            "Report - annotated",
            annotatedClipName("Report - annotated.pdf", suffix, untitled),
        )
        assertEquals(
            "Report - annotated",
            annotatedClipName("Report - annotated", suffix, untitled),
        )
    }

    @Test
    fun blankOriginal_fallsBackToUntitledPlusSuffix() {
        assertEquals(
            "$untitled$suffix",
            annotatedClipName("", suffix, untitled),
        )
        assertEquals(
            "$untitled$suffix",
            annotatedClipName("   ", suffix, untitled),
        )
    }

    @Test
    fun longOriginal_keepsSuffixAndShortensStem() {
        val result = annotatedClipName("a".repeat(200), suffix, untitled)

        assertTrue(result.endsWith(suffix))
        assertEquals(MAX_ANNOTATED_CLIP_NAME_LENGTH, result.length)
        assertFalse(result.contains("annotated - annotated"))
    }

    @Test
    fun localizedSuffix_isUsed() {
        assertEquals(
            "Bericht - annotiert",
            annotatedClipName("Bericht.pdf", " - annotiert", "(ohne Titel)"),
        )
    }
}
