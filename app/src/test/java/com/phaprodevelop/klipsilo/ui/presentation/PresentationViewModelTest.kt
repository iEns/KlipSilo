package com.phaprodevelop.klipsilo.ui.presentation

import android.app.Application
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PresentationViewModelTest {
    private lateinit var application: Application
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: PresentationViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        prefs = application.getSharedPreferences("reader_prefs", 0)
        prefs.edit().clear().commit()
        viewModel = PresentationViewModel(application)
    }

    @Test
    fun defaults_matchEmptyPrefs() {
        // Arrange — ViewModel constructed after prefs clear in setUp

        // Act
        val fontSize = viewModel.fontSize.value
        val isPageMode = viewModel.isPageMode.value
        val isDarkReader = viewModel.isDarkReader.value
        val isFullBrightness = viewModel.isFullBrightness.value

        // Assert
        assertEquals(20f, fontSize, 0.0f)
        assertFalse(isPageMode)
        assertFalse(isDarkReader)
        assertFalse(isFullBrightness)
    }

    @Test
    fun updateFontSize_updatesFlowAndPrefs() {
        // Arrange
        val size = 28f

        // Act
        viewModel.updateFontSize(size)

        // Assert
        assertEquals(28f, viewModel.fontSize.value, 0.0f)
        assertEquals(28f, prefs.getFloat("pres_font_size", 0f), 0.0f)
    }

    @Test
    fun togglePageMode_twice_returnsToFalse() {
        // Arrange
        assertFalse(viewModel.isPageMode.value)

        // Act
        viewModel.togglePageMode()

        // Assert
        assertTrue(viewModel.isPageMode.value)
        assertTrue(prefs.getBoolean("pres_page_mode", false))

        // Act
        viewModel.togglePageMode()

        // Assert
        assertFalse(viewModel.isPageMode.value)
        assertTrue(prefs.contains("pres_page_mode"))
        assertFalse(prefs.getBoolean("pres_page_mode", true))
    }

    @Test
    fun setTheme_persistsDarkAndFullBrightness() {
        // Arrange
        val dark = true
        val fullBrightness = true

        // Act
        viewModel.setTheme(dark = dark, fullBrightness = fullBrightness)

        // Assert
        assertTrue(viewModel.isDarkReader.value)
        assertTrue(viewModel.isFullBrightness.value)
        assertTrue(prefs.getBoolean("dark_reader", false))
        assertTrue(prefs.getBoolean("full_brightness", false))
    }
}
