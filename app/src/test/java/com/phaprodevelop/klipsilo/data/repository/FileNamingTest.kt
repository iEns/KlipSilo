package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamingTest {
    @Test
    fun sanitizeFileName_stripsReservedCharacters() {
        // Arrange
        val input = "  a/b\\c:d*e?f\"g<h>i|j.txt  "

        // Act
        val result = sanitizeFileName(input)

        // Assert
        assertEquals("a_b_c_d_e_f_g_h_i_j.txt", result)
    }

    @Test
    fun sanitizeFileName_truncatesTo100() {
        // Arrange
        val input = "a".repeat(150)

        // Act
        val result = sanitizeFileName(input)

        // Assert
        assertEquals(100, result.length)
        assertEquals("a".repeat(100), result)
    }

    @Test
    fun sanitizeFileName_blankAfterTrim_isBlank() {
        // Arrange
        val input = "   "

        // Act
        val result = sanitizeFileName(input)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun buildFileName_blankOriginal_usesPrefixPlusExt() {
        // Act
        val result = buildFileName("", "img_", ".jpg")

        // Assert
        assertEquals("img_.jpg", result)
    }

    @Test
    fun sanitizeFileName_and_buildFileName_stripSoftHyphens() {
        // #62: soft hyphens (U+00AD) must never leak into a file name.
        assertEquals("Ausgaberesolution.pdf", sanitizeFileName("Aus\u00ADgabe\u00ADreso\u00ADlution.pdf"))
        assertEquals("Ausgaberesolution.jpg", buildFileName("Aus\u00ADgabe\u00ADreso\u00ADlution.jpg", "img_", null))
    }

    @Test
    fun buildFileName_stripsPathAndCapsBaseAt30() {
        // Arrange
        val originalName = "very-long-name-that-exceeds-thirty-chars.png"

        // Act
        val result = buildFileName(originalName, "img_", ".jpg")

        // Assert
        val base = result.substringBeforeLast('.')
        assertTrue(base.length <= 30)
        assertTrue(result.endsWith(".jpg"))
        assertEquals("very-long-name-that-exceeds-th.jpg", result)
    }

    @Test
    fun buildPdfName_blank_becomesPdfDotPdf() {
        // Act
        val blankName = buildPdfName("")
        val colonName = buildPdfName("Report: 2024")

        // Assert
        assertEquals("pdf.pdf", blankName)
        assertEquals("Report_ 2024.pdf", colonName)
    }

    @Test
    fun buildTxtName_usesDefaultStemWhenBlank() {
        // Act
        val result = buildTxtName("", "clip")

        // Assert
        assertEquals("clip.txt", result)
    }

    @Test
    fun buildPageImageName_omitsPageSuffixWhenSinglePage() {
        // Act
        val result = buildPageImageName("Scan", 1, 1, "img")

        // Assert
        assertEquals("Scan.jpg", result)
    }

    @Test
    fun buildPageImageName_includesPageWhenMulti() {
        // Act
        val result = buildPageImageName("Scan.pdf", 3, 10, "img")

        // Assert
        assertEquals("Scan_p3.jpg", result)
    }
}
