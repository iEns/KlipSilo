package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarSaveActionTest {
    // 411dp of bar (Pixel 8) at 3x density, 56dp back icon area.
    private val pxPerDp = 3f
    private val bar = 411f * pxPerDp
    private val leading = 56f * pxPerDp

    private fun collapses(
        fontScale: Float,
        barWidthPx: Float,
        titleWidthPx: Float = 0f,
        actionLabelWidthPx: Float = 0f,
    ): Boolean =
        topBarActionCollapses(
            fontScale = fontScale,
            barWidthPx = barWidthPx,
            pxPerDp = pxPerDp,
            leadingWidthPx = leading,
            titleWidthPx = titleWidthPx,
            actionLabelWidthPx = actionLabelWidthPx,
        )

    @Test
    fun labelledActionStaysAtNormalScaleWithPlentyOfTitleSpace() {
        // "Gem" pill leaves most of the bar for the title.
        assertFalse(collapses(1f, bar, titleWidthPx = 90f, actionLabelWidthPx = 90f + 192f))
    }

    @Test
    fun actionCollapsesAtLargeFontScale() {
        // font scale 2.0 always collapses, even with a short label (#47).
        assertTrue(collapses(2f, bar, titleWidthPx = 60f, actionLabelWidthPx = 60f + 192f))
        assertTrue(collapses(1.5f, bar))
    }

    @Test
    fun actionCollapsesWhenTheTitleWouldGetLessThanFortyPercentOfTheBar() {
        // A wide "Speichern" pill at normal scale squeezes the title below
        // the minimum share of the bar (#47).
        val wideLabel = 300f * pxPerDp + 192f * pxPerDp
        assertTrue(collapses(1f, bar, titleWidthPx = 100f, actionLabelWidthPx = wideLabel))
        assertTrue(collapses(1f, bar, titleWidthPx = 100f, actionLabelWidthPx = wideLabel + 1f))
    }

    @Test
    fun actionStaysLabelledExactlyAtTheBoundary() {
        // 40% of the bar left for the title is still comfortable.
        val boundary = bar - leading - bar * TOP_BAR_MIN_TITLE_FRACTION
        assertFalse(collapses(1f, bar, titleWidthPx = 100f, actionLabelWidthPx = boundary))
    }

    @Test
    fun actionCollapsesOnUnknownBarWidthAtLargeScaleOnly() {
        assertFalse(collapses(1f, 0f, titleWidthPx = 600f))
        assertTrue(collapses(2f, 0f, titleWidthPx = 600f))
    }

    @Test
    fun compactBarsAlwaysCollapseRegardlessOfTheLabel() {
        // At 320dp the batch-2 retest showed the three screens disagreeing:
        // "Mark up" kept its "Speichern" pill while "Fill form" and "Edit
        // pages" collapsed. Jens decided ✓ is correct at 320dp, so the same
        // compact-bar rule must collapse every screen's action (#47).
        val bar320 = 320f * pxPerDp
        val labels = listOf(200f, 260f, 320f) // "Gem", "Speichern", "PDF speichern" widths
        for (label in labels) {
            assertTrue(
                topBarActionCollapses(
                    fontScale = 1f,
                    barWidthPx = bar320,
                    pxPerDp = pxPerDp,
                    leadingWidthPx = leading,
                    titleWidthPx = 100f * pxPerDp,
                    actionLabelWidthPx = label + 64f * pxPerDp,
                ),
            )
        }
    }

    @Test
    fun wideBarsStayLabelledAtNormalScale() {
        // Pixel 8 at normal scale (411dp): all three screens keep their
        // labelled Save button (#47).
        val bar411 = 411f * pxPerDp
        val labels = listOf(200f, 260f, 320f)
        for (label in labels) {
            assertFalse(
                topBarActionCollapses(
                    fontScale = 1f,
                    barWidthPx = bar411,
                    pxPerDp = pxPerDp,
                    leadingWidthPx = leading,
                    titleWidthPx = 100f * pxPerDp,
                    actionLabelWidthPx = label + 64f * pxPerDp,
                ),
            )
        }
    }

    @Test
    fun actionCollapsesWhenTheTitleNeedsMoreRoomThanIsLeft() {
        // Wide bar, narrow label, but a title whose floor width exceeds the
        // leftover space: collapse so the title keeps its room (#47).
        val titleSpace = bar - leading - 100f
        assertTrue(collapses(1f, bar, titleWidthPx = titleSpace + 1f, actionLabelWidthPx = 100f))
        assertFalse(collapses(1f, bar, titleWidthPx = titleSpace, actionLabelWidthPx = 100f))
    }
}
