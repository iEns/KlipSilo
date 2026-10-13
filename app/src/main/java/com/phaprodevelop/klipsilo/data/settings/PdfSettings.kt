package com.phaprodevelop.klipsilo.data.settings

import android.content.Context

enum class PdfFont {
    SYSTEM,
    SANS,
    SERIF,
    MONO,
}

data class PdfSettings(
    val marginTopBottomCm: Float = 2f,
    val marginLeftRightCm: Float = 2f,
    val fontSizePt: Float = 14f,
    val lineSpacing: Float = 1.4f,
    val maxOutputPx: Int = 3000,
    val pdfFont: PdfFont = PdfFont.SYSTEM,
) {
    val marginTopBottomPt: Float
        get() = marginTopBottomCm * CM_TO_PT
    val marginLeftRightPt: Float
        get() = marginLeftRightCm * CM_TO_PT

    companion object {
        private const val CM_TO_PT = 28.35f
    }
}

object PdfSettingsStore {
    private const val PREFS_NAME = "pdf_settings"
    private const val KEY_MARGIN_TOP_BOTTOM_CM = "margin_top_bottom_cm"
    private const val KEY_MARGIN_LEFT_RIGHT_CM = "margin_left_right_cm"
    private const val KEY_FONT_SIZE_PT = "font_size_pt"
    private const val KEY_LINE_SPACING = "line_spacing"
    private const val KEY_MAX_OUTPUT_PX = "max_output_px"
    private const val KEY_PDF_FONT = "pdf_font"

    fun load(context: Context): PdfSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val default = PdfSettings()
        return PdfSettings(
            marginTopBottomCm = prefs.getFloat(KEY_MARGIN_TOP_BOTTOM_CM, default.marginTopBottomCm),
            marginLeftRightCm = prefs.getFloat(KEY_MARGIN_LEFT_RIGHT_CM, default.marginLeftRightCm),
            fontSizePt = prefs.getFloat(KEY_FONT_SIZE_PT, default.fontSizePt),
            lineSpacing = prefs.getFloat(KEY_LINE_SPACING, default.lineSpacing),
            maxOutputPx = prefs.getInt(KEY_MAX_OUTPUT_PX, default.maxOutputPx),
            pdfFont =
                runCatching { PdfFont.valueOf(prefs.getString(KEY_PDF_FONT, default.pdfFont.name)!!) }
                    .getOrDefault(default.pdfFont),
        )
    }

    fun save(
        context: Context,
        settings: PdfSettings,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_MARGIN_TOP_BOTTOM_CM, settings.marginTopBottomCm)
            .putFloat(KEY_MARGIN_LEFT_RIGHT_CM, settings.marginLeftRightCm)
            .putFloat(KEY_FONT_SIZE_PT, settings.fontSizePt)
            .putFloat(KEY_LINE_SPACING, settings.lineSpacing)
            .putInt(KEY_MAX_OUTPUT_PX, settings.maxOutputPx)
            .putString(KEY_PDF_FONT, settings.pdfFont.name)
            .apply()
    }
}
