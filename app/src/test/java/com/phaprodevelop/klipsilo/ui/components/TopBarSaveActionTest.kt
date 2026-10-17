package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarSaveActionTest {
    private val bar = 1080f // 360dp at 3x density
    private val leading = 168f // 56dp back icon area

    @Test
    fun labelledActionStaysAtNormalScaleWithPlentyOfTitleSpace() {
        // "Gem" pill leaves most of the bar for the title.
        assertFalse(topBarActionCollapses(1f, bar, leading, 90f + 192f))
    }

    @Test
    fun actionCollapsesAtLargeFontScale() {
        // font scale 2.0 always collapses, even with a short label (#47).
        assertTrue(topBarActionCollapses(2f, bar, leading, 60f + 192f))
        assertTrue(topBarActionCollapses(1.5f, bar, leading, 0f))
    }

    @Test
    fun actionCollapsesWhenTheTitleWouldGetLessThanFortyPercentOfTheBar() {
        // A wide "Speichern" pill at normal scale squeezes the title below
        // the minimum share of the bar (#47).
        val wideLabel = 300f + 192f
        assertTrue(topBarActionCollapses(1f, bar, leading, wideLabel))
        assertTrue(topBarActionCollapses(1f, bar, leading, wideLabel + 1f))
    }

    @Test
    fun actionStaysLabelledExactlyAtTheBoundary() {
        // 40% of the bar left for the title is still comfortable.
        val boundary = bar - leading - bar * TOP_BAR_MIN_TITLE_FRACTION
        assertFalse(topBarActionCollapses(1f, bar, leading, boundary))
    }

    @Test
    fun actionCollapsesOnUnknownBarWidthAtLargeScaleOnly() {
        assertFalse(topBarActionCollapses(1f, 0f, leading, 600f))
        assertTrue(topBarActionCollapses(2f, 0f, leading, 600f))
    }
}
