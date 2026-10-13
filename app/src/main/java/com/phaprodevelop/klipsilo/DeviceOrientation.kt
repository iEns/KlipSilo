package com.phaprodevelop.klipsilo

import android.app.Activity
import android.content.pm.ActivityInfo

private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600

fun Activity.applyPhonePortraitLock() {
    requestedOrientation =
        if (resources.configuration.smallestScreenWidthDp >= LARGE_SCREEN_SMALLEST_WIDTH_DP) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
}

fun Activity.allowSensorOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
}
