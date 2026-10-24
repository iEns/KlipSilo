package com.phaprodevelop.klipsilo.ui.screen

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.settings.PdfSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #96: the PDF knobs start collapsed and expand from the PDF & export row. */
@RunWith(RobolectricTestRunner::class)
class PdfExportSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pdfSettingsAreCollapsedUntilTheRowIsTapped() {
        composeRule.setContent {
            MaterialTheme {
                PdfExportSection(pdfSettings = PdfSettings(), onSettingsChange = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_pdf_export)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.pdf_setting_font_family)).assertDoesNotExist()

        composeRule.onNodeWithText(context.getString(R.string.settings_pdf_export)).performClick()

        listOf(
            R.string.pdf_setting_font_family,
            R.string.pdf_setting_margin_top_bottom,
            R.string.pdf_setting_margin_left_right,
            R.string.pdf_setting_font_size,
            R.string.pdf_setting_line_spacing,
            R.string.pdf_setting_max_output,
        ).forEach { label ->
            composeRule.onNodeWithText(context.getString(label)).assertExists()
        }
    }
}
