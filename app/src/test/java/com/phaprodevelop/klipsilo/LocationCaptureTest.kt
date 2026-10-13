package com.phaprodevelop.klipsilo

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class LocationCaptureTest {
    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    @Test
    fun hasLocationPermission_none_isFalse() {
        // Arrange: both permissions denied in setUp

        // Act / Assert
        assertFalse(hasLocationPermission(app))
    }

    @Test
    fun hasLocationPermission_onlyCoarse_isTrue() {
        // Arrange
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Act / Assert
        assertTrue(hasLocationPermission(app))
    }

    @Test
    fun hasLocationPermission_onlyFine_isTrue() {
        // Arrange
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        // Act / Assert
        assertTrue(hasLocationPermission(app))
    }

    @Test
    fun wasLocationPermissionAsked_defaultFalse_thenTrueAfterMark() {
        // Arrange / Act / Assert — prefs klipsilo_perms / location_permission_requested
        assertFalse(wasLocationPermissionAsked(app))
        assertFalse(
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOCATION_ASKED, false),
        )

        markLocationPermissionAsked(app)

        assertTrue(wasLocationPermissionAsked(app))
        assertTrue(
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOCATION_ASKED, false),
        )
    }

    private companion object {
        const val PREFS = "klipsilo_perms"
        const val KEY_LOCATION_ASKED = "location_permission_requested"
    }
}
