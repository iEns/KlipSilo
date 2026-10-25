package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/** The crop frame -> bitmap rectangle conversion for OCR from an image (#82). */
class CropBitmapRectTest {
    @Test
    fun roundedEdges_includeTheFractionalRightAndBottomPixels() {
        val rect =
            cropBitmapRect(
                imageWidth = 100,
                imageHeight = 100,
                cropX = 0.105f,
                cropY = 0.105f,
                cropWidth = 0.2f,
                cropHeight = 0.2f,
                padPx = 0,
            )
        assertEquals(10, rect.left)
        assertEquals(10, rect.top)
        // 0.305 * 100 = 30.5: ceil keeps the trailing half pixel inside the crop.
        assertEquals(21, rect.width)
        assertEquals(21, rect.height)
        assertEquals(31, rect.left + rect.width)
        assertEquals(31, rect.top + rect.height)
    }

    @Test
    fun padding_expandsTheCropWithoutLeavingTheImage() {
        val full =
            cropBitmapRect(
                imageWidth = 100,
                imageHeight = 50,
                cropX = 0f,
                cropY = 0f,
                cropWidth = 1f,
                cropHeight = 1f,
                padPx = 4,
            )
        assertEquals(CropBitmapRect(0, 0, 100, 50), full)

        val inner =
            cropBitmapRect(
                imageWidth = 100,
                imageHeight = 50,
                cropX = 0.2f,
                cropY = 0.2f,
                cropWidth = 0.6f,
                cropHeight = 0.6f,
                padPx = 4,
            )
        assertEquals(16, inner.left)
        assertEquals(6, inner.top)
        assertEquals(84, inner.left + inner.width)
        assertEquals(44, inner.top + inner.height)
    }

    @Test
    fun cropPastTheRightEdge_isClampedToTheImage() {
        val rect =
            cropBitmapRect(
                imageWidth = 100,
                imageHeight = 80,
                cropX = 0.95f,
                cropY = 0.1f,
                cropWidth = 0.2f,
                cropHeight = 0.2f,
                padPx = 4,
            )
        assertEquals(91, rect.left)
        assertEquals(100, rect.left + rect.width)
        assertEquals(28, rect.top + rect.height)
    }

    @Test
    fun tinyOrEmptyCrops_stillProduceAtLeastOnePixel() {
        val rect =
            cropBitmapRect(
                imageWidth = 10,
                imageHeight = 10,
                cropX = 0.5f,
                cropY = 0.5f,
                cropWidth = 0f,
                cropHeight = 0f,
                padPx = 0,
            )
        assertEquals(1, rect.width)
        assertEquals(1, rect.height)
    }
}
