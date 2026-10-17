@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePresetsTest {
    @Test
    fun nearestPreset_snapsToTheClosestDot() {
        assertEquals(0.002f, nearestStrokePreset(0.001f))
        assertEquals(0.005f, nearestStrokePreset(0.004f))
        assertEquals(0.01f, nearestStrokePreset(0.012f))
        assertEquals(0.018f, nearestStrokePreset(0.02f))
    }

    @Test
    fun markupWidthLadder_isFineEnoughAndCoversTheOldSmlRange() {
        assertTrue(MARKUP_WIDTHS.size in 8..12)
        assertTrue(MARKUP_WIDTHS.first() <= 0.003f)
        assertTrue(MARKUP_WIDTHS.last() > 0.016f)
        for (i in 1 until MARKUP_WIDTHS.size) {
            assertTrue(MARKUP_WIDTHS[i] > MARKUP_WIDTHS[i - 1])
        }
    }

    @Test
    fun markupWidthLadder_keepsTheOldSmlPresetsExact() {
        assertEquals(0.003f, nearestMarkupWidth(0.003f))
        assertEquals(0.008f, nearestMarkupWidth(0.008f))
        assertEquals(0.016f, nearestMarkupWidth(0.016f))
    }

    @Test
    fun markupWidthStepIndex_snapsIntoTheLadder() {
        assertEquals(0, markupWidthStepIndex(0f))
        assertEquals(MARKUP_WIDTHS.size - 1, markupWidthStepIndex(9.9f))
    }

    @Test
    fun markupWidthStep_isClampedAtTheLadderEnds() {
        assertEquals(MARKUP_WIDTHS.first(), markupWidthStep(-3))
        assertEquals(MARKUP_WIDTHS.first(), markupWidthStep(0))
        assertEquals(MARKUP_WIDTHS.last(), markupWidthStep(MARKUP_WIDTHS.size))
        assertEquals(MARKUP_WIDTHS.last(), markupWidthStep(99))
    }

    @Test
    fun markupWidthSteps_stayOneStepApart() {
        val index = 4
        assertEquals(MARKUP_WIDTHS[index - 1], markupWidthStep(index - 1))
        assertEquals(MARKUP_WIDTHS[index], markupWidthStep(index))
        assertEquals(MARKUP_WIDTHS[index + 1], markupWidthStep(index + 1))
    }

    @Test
    fun oldSmlWidthPreferences_migrateToNearestStepWithoutLoss() {
        // The stored fraction from the old S/M/L presets snaps back to itself.
        listOf(0.003f, 0.008f, 0.016f).forEach { old ->
            assertEquals(old, markupWidthStep(markupWidthStepIndex(old)))
        }
        // An off-ladder legacy value (4 dots) rounds to the nearest step (ties
        // resolve to the lower step) and stays inside the ladder.
        assertEquals(0.004f, markupWidthStep(markupWidthStepIndex(0.0045f)))
    }

    @Test
    fun textSizeLadder_keepsTheOldSmlSizesExact_andIsClamped() {
        assertEquals(0.014f, nearestMarkupTextSize(0.014f))
        assertEquals(0.022f, nearestMarkupTextSize(0.022f))
        assertEquals(0.034f, nearestMarkupTextSize(0.034f))
        assertEquals(0.014f, markupTextSizeStep(markupTextSizeStepIndex(0.001f)))
        assertEquals(0.034f, markupTextSizeStep(markupTextSizeStepIndex(9.9f)))
    }
}
