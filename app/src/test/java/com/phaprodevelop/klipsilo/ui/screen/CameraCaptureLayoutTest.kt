package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The live-camera layout skeleton (#79): the preview and the shutter must be
 * siblings, never overlapping, at any font scale — here font scale 2 with a
 * placeholder box standing in for the camera (no emulator in this round).
 */
@RunWith(RobolectricTestRunner::class)
class CameraCaptureLayoutTest {
    @get:org.junit.Rule
    val composeRule = createComposeRule()

    private fun setContent(isLandscape: Boolean) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
            ) {
                CameraCaptureLayout(
                    isLandscape = isLandscape,
                    header = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color.Black)
                                .testTag("header"),
                        )
                    },
                    preview = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray)
                                .testTag("preview"),
                        )
                    },
                    bar = {
                        CameraShutterBar(isLandscape = isLandscape) {
                            CameraShutterButton(
                                contentDescription = "Auslösen",
                                enabled = true,
                                onClick = {},
                                modifier = Modifier.testTag("shutter"),
                            )
                        }
                    },
                )
            }
        }
    }

    @Test
    fun portrait_previewAndShutterNeverIntersect_atFontScale2() {
        setContent(isLandscape = false)
        composeRule.onNodeWithTag("preview").assertExists()
        val preview = bounds("preview")
        val shutter = bounds("shutter")
        assertFalse("shutter must not overlap the preview", preview.overlaps(shutter))
        // Portrait: the shutter sits in its own bar below the preview.
        assertTrue("shutter below the preview", shutter.top >= preview.bottom)
        assertShutterAtLeast48dp(shutter)
    }

    @Test
    fun landscape_previewAndShutterNeverIntersect_atFontScale2() {
        setContent(isLandscape = true)
        composeRule.onNodeWithTag("preview").assertExists()
        val preview = bounds("preview")
        val shutter = bounds("shutter")
        assertFalse("shutter must not overlap the preview", preview.overlaps(shutter))
        // Landscape: the shutter sits in its own end-side bar beside the preview.
        assertTrue("shutter right of the preview", shutter.left >= preview.right)
        assertShutterAtLeast48dp(shutter)
    }

    private fun assertShutterAtLeast48dp(shutter: DpRect) {
        assertTrue("shutter target ≥ 48dp wide", shutter.right - shutter.left >= 48.dp)
        assertTrue("shutter target ≥ 48dp tall", shutter.bottom - shutter.top >= 48.dp)
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun DpRect.overlaps(other: DpRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom
}
