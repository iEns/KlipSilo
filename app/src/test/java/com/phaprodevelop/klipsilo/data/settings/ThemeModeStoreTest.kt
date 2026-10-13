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
class ThemeModeStoreTest {
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
    fun load_freshPrefs_returnsSystem() {
        // Arrange: empty theme_mode prefs from setUp

        // Act
        val mode = ThemeModeStore.load(context)

        // Assert
        assertEquals(ThemeMode.SYSTEM, mode)
    }

    @Test
    fun save_darkThenLight_roundTrips() {
        // Arrange
        ThemeModeStore.save(context, ThemeMode.DARK)

        // Act
        val dark = ThemeModeStore.load(context)

        // Assert
        assertEquals(ThemeMode.DARK, dark)

        // Arrange
        ThemeModeStore.save(context, ThemeMode.LIGHT)

        // Act
        val light = ThemeModeStore.load(context)

        // Assert
        assertEquals(ThemeMode.LIGHT, light)
    }

    @Test
    fun load_invalidMode_returnsSystem() {
        // Arrange
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, "NOT_A_THEME")
            .commit()

        // Act
        val mode = ThemeModeStore.load(context)

        // Assert
        assertEquals(ThemeMode.SYSTEM, mode)
    }

    private companion object {
        const val PREFS_NAME = "theme_mode"
        const val KEY_MODE = "mode"
    }
}
