@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The image viewer's bounded decode (#66): the sample size keeps the
 * decoded bitmap inside the view size and under the decode-pixel cap, so a
 * very tall screenshot or a huge photo can no longer produce a >100 MB
 * bitmap that crashes when drawn.
 */
class ImageSampleSizeTest {
    @Test
    fun smallImageStaysAtOne() {
        assertEquals(1, imageSampleSize(1080, 800, 1080, 2400))
    }

    @Test
    fun wideCameraPhotoSamplesByTheWidth() {
        // 50 MP: 8160×6144 into a 1080×2400 view → width sample 4 (#66).
        assertEquals(4, imageSampleSize(8160, 6144, 1080, 2400))
    }

    @Test
    fun veryTallScreenshotSamplesByTheHeight() {
        // A 1080×24000 long screenshot used to decode at full size — 100+ MB,
        // "Canvas: trying to draw too large bitmap" (#66).
        assertEquals(8, imageSampleSize(1080, 24000, 1080, 2400))
    }

    @Test
    fun pixelCapDoublesTheSampleWhenBothSamplesAreSmall() {
        // 10000×20000 with a wide destination: the per-dimension samples
        // leave 50 MP decoded — the cap must double the sample to 16 MP.
        val sample = imageSampleSize(10000, 20000, 10000, 10000)
        val decoded = 10000L / sample * (20000L / sample)
        org.junit.Assert.assertTrue(decoded <= MAX_IMAGE_DECODE_PIXELS)
    }

    @Test
    fun corruptImageBoundsSampleAtOne() {
        // 0×0 (unreadable bounds): the caller rejects the decode; no divide
        // by zero and no negative sample.
        assertEquals(1, imageSampleSize(0, 0, 1080, 2400))
        assertEquals(1, imageSampleSize(8160, 6144, 0, 0))
    }

    @Test
    fun exactPowerOfTwoSamplesFit() {
        assertEquals(4, imageSampleSize(4096, 4096, 1024, 1024))
    }
}
