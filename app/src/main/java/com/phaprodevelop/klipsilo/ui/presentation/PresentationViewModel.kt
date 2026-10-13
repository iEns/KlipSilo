package com.phaprodevelop.klipsilo.ui.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PresentationViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    private val _fontSize = MutableStateFlow(prefs.getFloat("pres_font_size", 20f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _isPageMode = MutableStateFlow(prefs.getBoolean("pres_page_mode", false))
    val isPageMode: StateFlow<Boolean> = _isPageMode.asStateFlow()

    private val _isDarkReader = MutableStateFlow(prefs.getBoolean("dark_reader", false))
    val isDarkReader: StateFlow<Boolean> = _isDarkReader.asStateFlow()

    private val _isFullBrightness = MutableStateFlow(prefs.getBoolean("full_brightness", false))
    val isFullBrightness: StateFlow<Boolean> = _isFullBrightness.asStateFlow()

    fun updateFontSize(size: Float) {
        _fontSize.value = size
        prefs.edit().putFloat("pres_font_size", size).apply()
    }

    fun togglePageMode() {
        _isPageMode.value = !_isPageMode.value
        prefs.edit().putBoolean("pres_page_mode", _isPageMode.value).apply()
    }

    fun setTheme(
        dark: Boolean,
        fullBrightness: Boolean,
    ) {
        _isDarkReader.value = dark
        _isFullBrightness.value = fullBrightness
        prefs.edit()
            .putBoolean("dark_reader", dark)
            .putBoolean("full_brightness", fullBrightness)
            .apply()
    }
}
