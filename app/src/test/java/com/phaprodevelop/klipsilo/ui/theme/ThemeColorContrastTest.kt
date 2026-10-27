package com.phaprodevelop.klipsilo.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Guards the WCAG AA contrast of the theme's primary colour pairs (#38).
 * Threshold: 4.5:1 for normal text, applied to every primary-related pair,
 * in both the light and the dark scheme.
 */
class ThemeColorContrastTest {
    private fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linear(channel = color.red) +
            0.7152 * linear(channel = color.green) +
            0.0722 * linear(channel = color.blue)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val lighter = maxOf(luminance(a), luminance(b))
        val darker = minOf(luminance(a), luminance(b))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertAA(
        label: String,
        a: Color,
        b: Color,
    ) {
        val ratio = contrast(a, b)
        assertTrue("$label contrast is $ratio:1, needs at least $AA_RATIO:1", ratio >= AA_RATIO)
    }

    @Test
    fun lightPrimaryOnOnPrimary_meetsAA() {
        assertAA("light primary/onPrimary", SnipPrimary, SnipOnPrimary)
    }

    @Test
    fun lightPrimaryOnBackground_meetsAA() {
        assertAA("light primary/background", SnipPrimary, SnipBackground)
    }

    @Test
    fun lightPrimaryOnSurface_meetsAA() {
        assertAA("light primary/surface", SnipPrimary, SnipSurface)
    }

    @Test
    fun lightPrimaryContainerOnOnPrimaryContainer_meetsAA() {
        assertAA("light primaryContainer/onPrimaryContainer", SnipPrimaryContainer, SnipOnPrimaryContainer)
    }

    @Test
    fun darkPrimaryOnOnPrimary_meetsAA() {
        assertAA("dark primary/onPrimary", SnipDarkPrimary, SnipDarkOnPrimary)
    }

    @Test
    fun darkPrimaryOnBackground_meetsAA() {
        assertAA("dark primary/background", SnipDarkPrimary, SnipDarkBackground)
    }

    @Test
    fun darkPrimaryOnSurface_meetsAA() {
        assertAA("dark primary/surface", SnipDarkPrimary, SnipDarkSurface)
    }

    @Test
    fun darkPrimaryContainerOnOnPrimaryContainer_meetsAA() {
        assertAA("dark primaryContainer/onPrimaryContainer", SnipDarkPrimaryContainer, SnipDarkOnPrimaryContainer)
    }

    private companion object {
        const val AA_RATIO = 4.5
    }
}
