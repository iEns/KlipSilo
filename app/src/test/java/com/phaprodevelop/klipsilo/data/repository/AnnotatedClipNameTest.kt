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

    @Test
    fun copySuffix_stripsExtensionAndAppendsSuffix() {
        assertEquals(
            "Report - copy",
            copiedClipName("Report.pdf", " - copy", untitled),
        )
    }

    @Test
    fun copySuffix_isNotAppendedTwice() {
        assertEquals(
            "Report - copy",
            copiedClipName("Report - copy.pdf", " - copy", untitled),
        )
        assertEquals(
            "Report - copy",
            copiedClipName("Report - copy", " - copy", untitled),
        )
    }

    @Test
    fun copySuffix_blankOriginal_fallsBackToUntitled() {
        assertEquals(
            "$untitled - copy",
            copiedClipName("   ", " - copy", untitled),
        )
    }

    @Test
    fun copySuffix_longOriginal_keepsSuffix() {
        val result = copiedClipName("a".repeat(200), " - copy", untitled)

        assertTrue(result.endsWith(" - copy"))
        assertEquals(MAX_ANNOTATED_CLIP_NAME_LENGTH, result.length)
    }

    @Test
    fun pdfPageName_appendsOneBasedPageAndStripsExtension() {
        assertEquals("Report_p1", pdfPageClipName("Report.pdf", null, 1, untitled))
        assertEquals("Report_p3", pdfPageClipName("Report", null, 3, untitled))
        assertEquals(
            "Quarterly Report_p3",
            pdfPageClipName("Quarterly Report.pdf", null, 3, untitled),
        )
    }

    @Test
    fun pdfPageName_doesNotAppendThePageSuffixTwice() {
        assertEquals(
            "Report_p3",
            pdfPageClipName("Report_p3.pdf", null, 3, untitled),
        )
    }

    @Test
    fun pdfPageName_blankOrGenericPreviewUsesSourceFileName() {
        assertEquals(
            "Quarterly Report_p3",
            pdfPageClipName("", "/tmp/Quarterly Report.pdf", 3, untitled),
        )
        assertEquals(
            "Quarterly Report_p3",
            pdfPageClipName("pdf", "/tmp/Quarterly Report.pdf", 3, untitled),
        )
        assertEquals(
            "Quarterly Report_p3",
            pdfPageClipName("PDF", "/tmp/Quarterly Report.pdf", 3, untitled),
        )
    }

    @Test
    fun pdfPageName_longStemKeepsPageSuffix() {
        val result = pdfPageClipName("a".repeat(200) + ".pdf", null, 12, untitled)

        assertTrue(result.endsWith("_p12"))
        assertEquals(MAX_ANNOTATED_CLIP_NAME_LENGTH, result.length)
    }
}
