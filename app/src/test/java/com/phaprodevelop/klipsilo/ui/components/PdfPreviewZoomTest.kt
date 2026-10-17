package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPreviewZoomTest {
    @Test
    fun `clamps scale between 1x and 5x`() {
        assertEquals(1f, clampScale(0.3f))
        assertEquals(1f, clampScale(PREVIEW_MIN_SCALE))
        assertEquals(2.5f, clampScale(2.5f))
        assertEquals(5f, clampScale(9f))
        assertEquals(1f, clampScale(Float.NaN))
    }

    @Test
    fun `keeps the page centered when it is smaller than the viewport`() {
        assertEquals(0f, clampOffset(30f, scaledSize = 200f, viewportSize = 400f))
        assertEquals(0f, clampOffset(-500f, scaledSize = 200f, viewportSize = 400f))
    }

    @Test
    fun `clamps offsets so the page cannot leave the viewport`() {
        // 600px page in a 400px viewport may move 100px in each direction at most.
        assertEquals(100f, clampOffset(250f, scaledSize = 600f, viewportSize = 400f))
        assertEquals(-100f, clampOffset(-250f, scaledSize = 600f, viewportSize = 400f))
        assertEquals(42f, clampOffset(42f, scaledSize = 600f, viewportSize = 400f))
    }

    @Test
    fun `pan step keeps the tapped page point fixed while zooming`() {
        val start = PreviewZoomState(scale = 1f, offsetX = 0f, offsetY = 0f)
        // Tap point 120px right of the viewport center.
        val zoomed =
            transformPreviewZoom(
                current = start,
                zoomDelta = 2f,
                panX = 0f,
                panY = 0f,
                focalX = 120f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        assertEquals(2f, zoomed.scale)
        // The focal point maps to (offset + scale * local); keep it at 120px: local = (120-0)/1.
        val focalLocal = (120f - start.offsetX) / start.scale
        assertEquals(120f, zoomed.offsetX + zoomed.scale * focalLocal)
    }

    @Test
    fun `pan step accumulates and then clamps`() {
        val start = PreviewZoomState(scale = 2f, offsetX = 0f, offsetY = 0f)
        val moved =
            transformPreviewZoom(
                current = start,
                zoomDelta = 1f,
                panX = 40f,
                panY = -1000f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        // Scaled page 800x1000 in a 400x300 viewport: max pan is 200 and 350.
        assertEquals(40f, moved.offsetX)
        assertEquals(-350f, moved.offsetY)
        val again =
            transformPreviewZoom(
                current = moved,
                zoomDelta = 1f,
                panX = 10_000f,
                panY = 0f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        assertEquals(200f, again.offsetX)
    }

    @Test
    fun `zoom step cannot exceed the maximum scale`() {
        val start = PreviewZoomState(scale = 4.8f)
        val zoomed =
            transformPreviewZoom(
                current = start,
                zoomDelta = 3f,
                panX = 0f,
                panY = 0f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        assertEquals(PREVIEW_MAX_SCALE, zoomed.scale)
    }

    @Test
    fun `zero and invalid zoom deltas only pan`() {
        val start = PreviewZoomState(scale = 2f, offsetX = 10f)
        assertEquals(
            PreviewZoomState(scale = 2f, offsetX = 15f, offsetY = 5f),
            transformPreviewZoom(start, 0f, 5f, 5f, 0f, 0f, 400f, 300f, 400f, 500f),
        )
        assertEquals(
            PreviewZoomState(scale = 2f, offsetX = 15f, offsetY = 5f),
            transformPreviewZoom(start, Float.NaN, 5f, 5f, 0f, 0f, 400f, 300f, 400f, 500f),
        )
    }

    @Test
    fun `double tap at 1x zooms to the double tap scale around the tap point`() {
        val zoomed =
            doubleTapPreviewZoom(
                current = PreviewZoomState(),
                focalX = 180f,
                focalY = 90f,
                viewportWidth = 400f,
                viewportHeight = 200f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        assertEquals(PREVIEW_DOUBLE_TAP_SCALE, zoomed.scale)
        val factor = PREVIEW_DOUBLE_TAP_SCALE
        assertEquals(180f * (1f - factor), zoomed.offsetX)
        assertEquals(90f * (1f - factor), zoomed.offsetY)
    }

    @Test
    fun `double tap clamps near the edges`() {
        val zoomed =
            doubleTapPreviewZoom(
                current = PreviewZoomState(),
                focalX = 10_000f,
                focalY = -10_000f,
                viewportWidth = 400f,
                viewportHeight = 200f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        // Page 400x500 at 2.5x is 1000x1250 in a 400x200 viewport: max 300 and 525.
        // A focal point right of center pushes the page left; one above center pushes it down.
        assertEquals(-300f, zoomed.offsetX)
        assertEquals(525f, zoomed.offsetY)
    }

    @Test
    fun `double tap while zoomed resets to 1x centered`() {
        assertEquals(
            PreviewZoomState(),
            doubleTapPreviewZoom(
                current = PreviewZoomState(scale = 2f, offsetX = 30f, offsetY = -40f),
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 200f,
                pageWidth = 400f,
                pageHeight = 500f,
            ),
        )
    }

    @Test
    fun `re-clamps after the viewport shrinks`() {
        val zoomed = PreviewZoomState(scale = 2f, offsetX = 400f, offsetY = -1000f)
        val clamped =
            clampPreviewZoom(zoomed, viewportWidth = 400f, viewportHeight = 200f, pageWidth = 400f, pageHeight = 500f)
        assertEquals(200f, clamped.offsetX)
        assertEquals(-400f, clamped.offsetY)
        assertEquals(2f, clamped.scale)
    }

    @Test
    fun `maps viewport taps back to page coordinates including the zoom`() {
        val viewportWidth = 400f
        val viewportHeight = 200f
        val pageWidth = 400f
        val pageHeight = 500f
        val zoom = PreviewZoomState(scale = 2.5f, offsetX = 100f, offsetY = -50f)
        for (x in listOf(0f, 100f, 200f, 399f)) {
            for (y in listOf(0f, 50f, 199f)) {
                val (localX, localY) =
                    previewPointToPageLocal(
                        zoom,
                        x,
                        y,
                        viewportWidth,
                        viewportHeight,
                        pageWidth,
                        pageHeight,
                    )
                // Round trip: the local point draws back exactly where it was tapped.
                val backX = viewportWidth / 2f + zoom.offsetX + zoom.scale * (localX - pageWidth / 2f)
                val backY = viewportHeight / 2f + zoom.offsetY + zoom.scale * (localY - pageHeight / 2f)
                assertEquals(x, backX, 0.01f)
                assertEquals(y, backY, 0.01f)
            }
        }
    }

    @Test
    fun `preview point mapping degenerates to identity at 1x`() {
        val (localX, localY) =
            previewPointToPageLocal(
                PreviewZoomState(),
                viewportX = 250f,
                viewportY = 120f,
                viewportWidth = 400f,
                viewportHeight = 200f,
                pageWidth = 400f,
                pageHeight = 500f,
            )
        assertEquals(250f, localX)
        assertEquals(270f, localY)
    }

    @Test
    fun `requests a sharper render once zoom settles`() {
        val first = previewRenderDim(currentDim = 0, scale = 1f)
        assertEquals(PREVIEW_BASE_RENDER_DIM, first)
        // Small zoom changes keep the current render.
        assertNull(previewRenderDim(currentDim = PREVIEW_BASE_RENDER_DIM, scale = 1.1f))
        // A meaningful zoom in re-renders sharper, capped by the memory limit.
        assertEquals(PREVIEW_MAX_RENDER_DIM, previewRenderDim(currentDim = PREVIEW_BASE_RENDER_DIM, scale = 2f))
        assertTrue(PREVIEW_MAX_RENDER_DIM > PREVIEW_BASE_RENDER_DIM)
        // Zooming back out re-renders the base resolution.
        assertEquals(PREVIEW_BASE_RENDER_DIM, previewRenderDim(currentDim = PREVIEW_MAX_RENDER_DIM, scale = 1f))
    }
}
