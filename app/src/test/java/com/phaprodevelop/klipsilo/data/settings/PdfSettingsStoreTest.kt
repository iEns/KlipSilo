@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfSettingsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun load_emptyPrefs_returnsDefaults() {
        // Arrange: empty pdf_settings prefs from setUp

        // Act
        val loaded = PdfSettingsStore.load(context)

        // Assert
        assertEquals(PdfSettings(), loaded)
    }

    @Test
    fun save_customSettings_roundTrips() {
        // Arrange
        val custom =
            PdfSettings(
                marginTopBottomCm = 3.5f,
                marginLeftRightCm = 1.25f,
                fontSizePt = 8f,
                lineSpacing = 1.8f,
                maxOutputPx = 1200,
                pdfFont = PdfFont.MONO,
            )

        // Act
        PdfSettingsStore.save(context, custom)
        val loaded = PdfSettingsStore.load(context)

        // Assert
        assertEquals(custom, loaded)
    }

    @Test
    fun load_invalidPdfFont_fallsBackToSystem_otherFieldsLoad() {
        // Arrange
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_MARGIN_TOP_BOTTOM_CM, 3.5f)
            .putFloat(KEY_MARGIN_LEFT_RIGHT_CM, 1.25f)
            .putFloat(KEY_FONT_SIZE_PT, 8f)
            .putFloat(KEY_LINE_SPACING, 1.8f)
            .putInt(KEY_MAX_OUTPUT_PX, 1200)
            .putString(KEY_PDF_FONT, "COMIC")
            .commit()

        // Act
        val loaded = PdfSettingsStore.load(context)

        // Assert
        assertEquals(PdfFont.SYSTEM, loaded.pdfFont)
        assertEquals(3.5f, loaded.marginTopBottomCm, 0.001f)
        assertEquals(1.25f, loaded.marginLeftRightCm, 0.001f)
        assertEquals(8f, loaded.fontSizePt, 0.001f)
        assertEquals(1.8f, loaded.lineSpacing, 0.001f)
        assertEquals(1200, loaded.maxOutputPx)
    }

    private companion object {
        const val PREFS_NAME = "pdf_settings"
        const val KEY_MARGIN_TOP_BOTTOM_CM = "margin_top_bottom_cm"
        const val KEY_MARGIN_LEFT_RIGHT_CM = "margin_left_right_cm"
        const val KEY_FONT_SIZE_PT = "font_size_pt"
        const val KEY_LINE_SPACING = "line_spacing"
        const val KEY_MAX_OUTPUT_PX = "max_output_px"
        const val KEY_PDF_FONT = "pdf_font"
    }
}
