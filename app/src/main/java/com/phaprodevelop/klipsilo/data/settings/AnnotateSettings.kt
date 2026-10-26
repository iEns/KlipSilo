package com.phaprodevelop.klipsilo.data.settings

import android.content.Context
import android.graphics.Color

data class AnnotateSettings(
    val textSizeFraction: Float = 0.013f,
    val textColor: Int = Color.BLACK,
    val strokeWidthFraction: Float = 0.008f,
    val lineColor: Int = Color.BLACK,
    val drawColor: Int = Color.BLACK,
    val highlightColor: Int = Color.YELLOW,
)

object AnnotateSettingsStore {
    private const val PREFS_NAME = "annotate_settings"
    private const val KEY_TEXT_SIZE_FRACTION = "text_size_fraction"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_STROKE_WIDTH_FRACTION = "stroke_width_fraction"
    private const val KEY_LINE_COLOR = "line_color"
    private const val KEY_DRAW_COLOR = "draw_color"
    private const val KEY_HIGHLIGHT_COLOR = "highlight_color"

    // Legacy shared pen colour. Kept read-only so existing installs migrate their
    // last line/draw colour into the per-tool keys on first load (#144).
    private const val KEY_STROKE_COLOR = "stroke_color"

    fun load(context: Context): AnnotateSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val default = AnnotateSettings()
        val legacyStrokeColor = prefs.getInt(KEY_STROKE_COLOR, default.lineColor)
        return AnnotateSettings(
            textSizeFraction = prefs.getFloat(KEY_TEXT_SIZE_FRACTION, default.textSizeFraction),
            textColor = prefs.getInt(KEY_TEXT_COLOR, default.textColor),
            strokeWidthFraction = prefs.getFloat(KEY_STROKE_WIDTH_FRACTION, default.strokeWidthFraction),
            lineColor = prefs.getInt(KEY_LINE_COLOR, legacyStrokeColor),
            drawColor = prefs.getInt(KEY_DRAW_COLOR, legacyStrokeColor),
            highlightColor = prefs.getInt(KEY_HIGHLIGHT_COLOR, default.highlightColor),
        )
    }

    fun save(
        context: Context,
        settings: AnnotateSettings,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_TEXT_SIZE_FRACTION, settings.textSizeFraction)
            .putInt(KEY_TEXT_COLOR, settings.textColor)
            .putFloat(KEY_STROKE_WIDTH_FRACTION, settings.strokeWidthFraction)
            .putInt(KEY_LINE_COLOR, settings.lineColor)
            .putInt(KEY_DRAW_COLOR, settings.drawColor)
            .putInt(KEY_HIGHLIGHT_COLOR, settings.highlightColor)
            .apply()
    }
}
