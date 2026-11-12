@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #127 retired PDF → HTML: a PDF clip's Convert & Edit surface has only the
 * two PDF rows, and HTML clips are File clips with HTML → PDF still offered.
 */
class PdfHtmlConversionActionsTest {
    @Test
    fun pdfConversionSurface_hasNoPdfToHtmlRow() {
        assertEquals(
            listOf(ClipSheetAction.PDF_TO_IMAGE, ClipSheetAction.PDF_TO_TEXT),
            conversionActionsFor(ContentType.PDF),
        )
        assertTrue(
            "PDF → HTML was retired in #127",
            ClipSheetAction.entries.none { it.name.contains("PDF_TO_HTML") },
        )
    }

    @Test
    fun htmlToPdf_isOfferedOnlyForHtmlFileClips() {
        assertTrue(offersHtmlToPdf(entry(ContentType.FILE, "/storage/page.html")))
        assertTrue(offersHtmlToPdf(entry(ContentType.FILE, "/storage/page.HTM")))
        assertFalse(offersHtmlToPdf(entry(ContentType.FILE, "/storage/page.txt")))
        assertFalse(offersHtmlToPdf(entry(ContentType.FILE, null)))
        // HTML is a File clip now; a non-File type with an .html path is not it.
        assertFalse(offersHtmlToPdf(entry(ContentType.TEXT, "/storage/page.html")))
        assertFalse(offersHtmlToPdf(entry(ContentType.PDF, "/storage/page.html")))
    }

    @Test
    fun fileConversionSurface_stillHasFileToTextOnly() {
        assertEquals(
            listOf(ClipSheetAction.FILE_TO_TEXT),
            conversionActionsFor(ContentType.FILE),
        )
    }

    private fun entry(
        contentType: ContentType,
        filePath: String?,
    ) = ClipboardEntry(
        id = 1L,
        contentType = contentType,
        filePath = filePath,
        previewText = "sample",
        timestamp = 0L,
    )
}
