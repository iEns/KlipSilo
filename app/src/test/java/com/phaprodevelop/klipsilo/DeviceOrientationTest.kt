package com.phaprodevelop.klipsilo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceOrientationTest {
    @Test
    fun applyPhonePortraitLock_sw320_locksPortrait() {
        // Arrange
        val activity = activityWithSmallestWidthDp(320)

        // Act
        activity.applyPhonePortraitLock()

        // Assert
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun applyPhonePortraitLock_sw600_unspecified() {
        // Arrange
        val activity = activityWithSmallestWidthDp(600)

        // Act
        activity.applyPhonePortraitLock()

        // Assert
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
    }

    @Test
    fun applyPhonePortraitLock_sw800_unspecified() {
        // Arrange
        val activity = activityWithSmallestWidthDp(800)

        // Act
        activity.applyPhonePortraitLock()

        // Assert
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
    }

    @Test
    fun allowSensorOrientation_setsSensor() {
        // Arrange
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        // Act
        activity.allowSensorOrientation()

        // Assert
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, activity.requestedOrientation)
    }

    private fun activityWithSmallestWidthDp(dp: Int): Activity {
        RuntimeEnvironment.setQualifiers("sw${dp}dp")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val config = Configuration(activity.resources.configuration)
        config.smallestScreenWidthDp = dp
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        assertEquals(dp, activity.resources.configuration.smallestScreenWidthDp)
        return activity
    }
}
