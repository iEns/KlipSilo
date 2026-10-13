@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfSettingsMathTest {
    @Test
    fun default_marginTopBottomPt_equalsTwoCmInPoints() {
        // Arrange
        val settings = PdfSettings()
        val expectedPt = 2f * 28.35f

        // Act
        val actualPt = settings.marginTopBottomPt

        // Assert
        assertEquals(expectedPt, actualPt, 0.001f)
        assertEquals(56.7f, actualPt, 0.001f)
    }

    @Test
    fun copy_marginLeftRightCm_half_convertsToPt() {
        // Arrange
        val settings =
            PdfSettings().copy(
                marginLeftRightCm = 0.5f,
            )

        // Act
        val actualPt = settings.marginLeftRightPt

        // Assert
        assertEquals(14.175f, actualPt, 0.001f)
    }

    @Test
    fun defaults_matchProductionValues() {
        // Arrange
        val settings = PdfSettings()

        // Act
        val fontSizePt = settings.fontSizePt
        val lineSpacing = settings.lineSpacing
        val maxOutputPx = settings.maxOutputPx
        val pdfFont = settings.pdfFont
        val marginTopBottomCm = settings.marginTopBottomCm
        val marginLeftRightCm = settings.marginLeftRightCm

        // Assert
        assertEquals(14f, fontSizePt, 0.001f)
        assertEquals(1.4f, lineSpacing, 0.001f)
        assertEquals(3000, maxOutputPx)
        assertEquals(PdfFont.SYSTEM, pdfFont)
        assertEquals(2f, marginTopBottomCm, 0.001f)
        assertEquals(2f, marginLeftRightCm, 0.001f)
    }
}
