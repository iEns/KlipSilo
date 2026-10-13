package com.phaprodevelop.klipsilo.data.settings

import android.content.Context
import android.graphics.Color

data class AnnotateSettings(
    val textSizeFraction: Float = 0.013f,
    val textColor: Int = Color.BLACK,
    val strokeWidthFraction: Float = 0.008f,
    val strokeColor: Int = Color.BLACK,
)

object AnnotateSettingsStore {
    private const val PREFS_NAME = "annotate_settings"
    private const val KEY_TEXT_SIZE_FRACTION = "text_size_fraction"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_STROKE_WIDTH_FRACTION = "stroke_width_fraction"
    private const val KEY_STROKE_COLOR = "stroke_color"

    fun load(context: Context): AnnotateSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val default = AnnotateSettings()
        return AnnotateSettings(
            textSizeFraction = prefs.getFloat(KEY_TEXT_SIZE_FRACTION, default.textSizeFraction),
            textColor = prefs.getInt(KEY_TEXT_COLOR, default.textColor),
            strokeWidthFraction = prefs.getFloat(KEY_STROKE_WIDTH_FRACTION, default.strokeWidthFraction),
            strokeColor = prefs.getInt(KEY_STROKE_COLOR, default.strokeColor),
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
            .putInt(KEY_STROKE_COLOR, settings.strokeColor)
            .apply()
    }
}
