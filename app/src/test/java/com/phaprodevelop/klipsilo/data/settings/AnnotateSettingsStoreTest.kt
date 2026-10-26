@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data.settings

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnnotateSettingsStoreTest {
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
        // Arrange: empty annotate_settings prefs from setUp

        // Act
        val loaded = AnnotateSettingsStore.load(context)

        // Assert
        assertEquals(AnnotateSettings(), loaded)
    }

    @Test
    fun save_customSettings_roundTripsPerToolColors() {
        // Arrange
        val custom =
            AnnotateSettings(
                textSizeFraction = 0.02f,
                textColor = Color.RED,
                strokeWidthFraction = 0.012f,
                lineColor = Color.BLUE,
                drawColor = Color.GREEN,
                highlightColor = Color.YELLOW,
            )

        // Act
        AnnotateSettingsStore.save(context, custom)
        val loaded = AnnotateSettingsStore.load(context)

        // Assert
        assertEquals(custom, loaded)
    }

    @Test
    fun load_legacyStrokeColorOnly_migratesLineAndDraw_highlightStaysYellow() {
        // Arrange: prefs written by the old shared strokeColor format
        val legacyStrokeColor = Color.GREEN
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_STROKE_COLOR, legacyStrokeColor)
            .commit()

        // Act
        val loaded = AnnotateSettingsStore.load(context)

        // Assert
        assertEquals(legacyStrokeColor, loaded.lineColor)
        assertEquals(legacyStrokeColor, loaded.drawColor)
        assertEquals(Color.YELLOW, loaded.highlightColor)
        assertEquals(AnnotateSettings().textColor, loaded.textColor)
    }

    private companion object {
        const val PREFS_NAME = "annotate_settings"
        const val KEY_STROKE_COLOR = "stroke_color"
    }
}
