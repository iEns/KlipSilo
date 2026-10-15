@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StrokePresetsTest {
    @Test
    fun nearestPreset_snapsToTheClosestDot() {
        assertEquals(0.002f, nearestStrokePreset(0.001f))
        assertEquals(0.005f, nearestStrokePreset(0.004f))
        assertEquals(0.01f, nearestStrokePreset(0.012f))
        assertEquals(0.018f, nearestStrokePreset(0.02f))
    }
}
