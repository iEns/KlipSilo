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
    fun `fit pan centers the page at fit-to-screen even with a stale offset`() {
        val clamped = clampFittedPan(300f, -500f, scale = 1f, 400f, 400f, 400f, 800f)
        assertEquals(PreviewZoomState(scale = 1f), clamped)
    }

    @Test
    fun `fit pan uses the letterboxed page size when clamping`() {
        // A 400x800 page fits as 200x400 in a 400x400 viewport: 1.1x is 220x440.
        // There is no horizontal room to pan yet, and only 20px vertically.
        val portrait = clampFittedPan(999f, -999f, scale = 1.1f, 400f, 400f, 400f, 800f)
        assertEquals(0f, portrait.offsetX, 0.01f)
        assertEquals(-20f, portrait.offsetY, 0.01f)

        // A 800x400 page fits as 400x200; 1.5x is 600x300, so only X has room (100px).
        val landscape = clampFittedPan(-999f, 999f, scale = 1.5f, 400f, 400f, 800f, 400f)
        assertEquals(-100f, landscape.offsetX, 0.01f)
        assertEquals(0f, landscape.offsetY, 0.01f)
    }

    @Test
    fun `fit pan keeps the page edge flush when zoomed in further`() {
        val clamped = clampFittedPan(10_000f, -10_000f, scale = 2f, 400f, 300f, 400f, 500f)
        // A 400x500 page fits as 240x300 in a 400x300 viewport; at 2x it is 480x600.
        assertEquals(40f, clamped.offsetX, 0.01f)
        assertEquals(-150f, clamped.offsetY, 0.01f)
        assertEquals(2f, clamped.scale)
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

    @Test
    fun `fitted zoom keeps the pinched point under the fingers`() {
        // 800x400 page in a 400x300 viewport fits as 400x200; at 2x it is 800x400.
        val zoomed =
            transformFittedZoom(
                current = PreviewZoomState(),
                zoomDelta = 2f,
                panX = 0f,
                panY = 0f,
                focalX = 100f,
                focalY = 20f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 800f,
                pageHeight = 400f,
            )
        assertEquals(2f, zoomed.scale)
        // offset = focal * (1 - factor); both are inside the 200/50px clamp range.
        assertEquals(-100f, zoomed.offsetX, 0.01f)
        assertEquals(-20f, zoomed.offsetY, 0.01f)
    }

    @Test
    fun `fitted pinch in returns to the same focal point`() {
        var state = PreviewZoomState()
        val start =
            transformFittedZoom(state, 2f, 0f, 0f, 80f, -40f, 400f, 300f, 800f, 400f)
        state = transformFittedZoom(start, 0.5f, 0f, 0f, 80f, -40f, 400f, 300f, 800f, 400f)
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.offsetX, 0.01f)
        assertEquals(0f, state.offsetY, 0.01f)
    }

    @Test
    fun `fitted zoom clamps the page edge flush with the viewport`() {
        val zoomed =
            transformFittedZoom(
                current = PreviewZoomState(scale = 4f),
                zoomDelta = 3f,
                panX = 10_000f,
                panY = -10_000f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 800f,
                pageHeight = 400f,
                maxScale = 8f,
            )
        assertEquals(8f, zoomed.scale)
        // 400x200 at 8x is 3200x1600: max pan is 1400 and 650.
        assertEquals(1400f, zoomed.offsetX, 0.01f)
        assertEquals(-650f, zoomed.offsetY, 0.01f)
    }

    @Test
    fun `fitted zoom ignores invalid deltas and caps at the viewer maximum`() {
        val start = PreviewZoomState(scale = 2f)
        for (delta in listOf(Float.NaN, 0f, -2f)) {
            assertEquals(
                start,
                transformFittedZoom(start, delta, 0f, 0f, 0f, 0f, 400f, 300f, 800f, 400f),
            )
        }
        assertEquals(
            PDF_VIEW_MAX_SCALE,
            transformFittedZoom(start, 100f, 0f, 0f, 0f, 0f, 400f, 300f, 800f, 400f, PDF_VIEW_MAX_SCALE).scale,
        )
    }

    @Test
    fun `non-finite state recovers to the centred fit instead of a blank page`() {
        assertEquals(0f, clampOffset(Float.NaN, scaledSize = 600f, viewportSize = 400f))
        assertEquals(PreviewZoomState(), clampFittedPan(Float.NaN, 0f, 1f, 400f, 400f, 400f, 800f))
        assertEquals(PreviewZoomState(), clampFittedPan(0f, 0f, Float.NaN, 400f, 400f, 400f, 800f))
        assertEquals(
            PreviewZoomState(),
            transformFittedZoom(PreviewZoomState(scale = Float.NaN), 2f, 0f, 0f, 0f, 0f, 400f, 300f, 800f, 400f),
        )
        // A single bad sample cannot leave a NaN offset behind; the next step recentres.
        val recovered =
            transformFittedZoom(
                current = PreviewZoomState(scale = 2f, offsetX = Float.NaN, offsetY = 0f),
                zoomDelta = 1f,
                panX = 10f,
                panY = 0f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 800f,
                pageHeight = 400f,
            )
        assertEquals(PreviewZoomState(), recovered)
    }

    @Test
    fun `scale clamps infinity to the preview maximum`() {
        assertEquals(PREVIEW_MAX_SCALE, clampScale(Float.POSITIVE_INFINITY))
        assertEquals(PREVIEW_MIN_SCALE, clampScale(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun `fit page rect letterboxes the page on the shorter axis`() {
        // 400x800 page in a 400x400 viewport fits 200x400, centred horizontally.
        val portrait = previewPageRect(400f, 400f, 400f, 800f)
        assertEquals(100f, portrait.left, 0.01f)
        assertEquals(0f, portrait.top, 0.01f)
        assertEquals(200f, portrait.width, 0.01f)
        assertEquals(400f, portrait.height, 0.01f)
    }

    @Test
    fun `fit width page rect fills the width and may extend past the top and bottom`() {
        // 400x800 page in a short 400x200 canvas: the width fills, height 800 extends beyond.
        val fitWidth = previewPageRect(400f, 200f, 400f, 800f, fitWidth = true)
        assertEquals(0f, fitWidth.left, 0.01f)
        assertEquals(-300f, fitWidth.top, 0.01f)
        assertEquals(400f, fitWidth.width, 0.01f)
        assertEquals(800f, fitWidth.height, 0.01f)
        // A stubby page is narrower than the canvas after fit-to-width.
        val landscape = previewPageRect(400f, 300f, 800f, 400f, fitWidth = true)
        assertEquals(400f, landscape.width, 0.01f)
        assertEquals(200f, landscape.height, 0.01f)
        assertEquals(50f, landscape.top, 0.01f)
    }

    @Test
    fun `fit width page rect falls back to the viewport for degenerate input`() {
        assertEquals(PreviewPageRect(0f, 0f, 0f, 0f), previewPageRect(0f, 0f, 400f, 800f, true))
        assertEquals(
            PreviewPageRect(0f, 0f, 400f, 300f),
            previewPageRect(400f, 300f, Float.NaN, 800f, true),
        )
    }

    @Test
    fun `fit width clamp allows panning the taller page at 1x`() {
        // Fit-page has no room at 1x; fit-width is 800px tall in a 300px viewport (250px each way).
        val fitPage = clampFittedPan(0f, 120f, 1f, 400f, 300f, 400f, 800f)
        assertEquals(0f, fitPage.offsetY, 0.01f)
        val fitWidth = clampFittedPan(999f, 999f, 1f, 400f, 300f, 400f, 800f, fitWidth = true)
        assertEquals(0f, fitWidth.offsetX, 0.01f)
        assertEquals(250f, fitWidth.offsetY, 0.01f)
        val clampedDown = clampFittedPan(0f, -999f, 1f, 400f, 300f, 400f, 800f, fitWidth = true)
        assertEquals(-250f, clampedDown.offsetY, 0.01f)
    }

    @Test
    fun `fit width zoom keeps the page edges flush`() {
        // Fit-width 400x800 at 1.5x is 600x1200 in a 400x300 viewport: 100 and 450 px of pan.
        val zoomed =
            transformFittedZoom(
                current = PreviewZoomState(),
                zoomDelta = 1.5f,
                panX = 10_000f,
                panY = -10_000f,
                focalX = 0f,
                focalY = 0f,
                viewportWidth = 400f,
                viewportHeight = 300f,
                pageWidth = 400f,
                pageHeight = 800f,
                fitWidth = true,
            )
        assertEquals(1.5f, zoomed.scale, 0.001f)
        assertEquals(100f, zoomed.offsetX, 0.01f)
        assertEquals(-450f, zoomed.offsetY, 0.01f)
    }

    @Test
    fun `reveal pan keeps the field above the keyboard margin`() {
        // Fit-width page in a 300px canvas: layer top -250, a field at 90% is 470..490.
        val offset =
            revealOffsetY(
                currentOffsetY = 0f,
                targetTop = 470f,
                targetBottom = 490f,
                scale = 1f,
                viewportHeight = 300f,
                scaledPageHeight = 800f,
                topMargin = 16f,
                bottomMargin = 16f,
            )
        assertEquals(-206f, offset, 0.01f)
    }

    @Test
    fun `reveal pan does not move when the field is already visible`() {
        val offset =
            revealOffsetY(
                currentOffsetY = 0f,
                targetTop = 150f,
                targetBottom = 170f,
                scale = 1f,
                viewportHeight = 300f,
                scaledPageHeight = 800f,
                topMargin = 16f,
                bottomMargin = 16f,
            )
        assertEquals(0f, offset, 0.01f)
        // An existing user pan inside the valid range is kept.
        val kept =
            revealOffsetY(
                currentOffsetY = -100f,
                targetTop = 150f,
                targetBottom = 170f,
                scale = 1f,
                viewportHeight = 300f,
                scaledPageHeight = 800f,
                topMargin = 16f,
                bottomMargin = 16f,
            )
        assertEquals(-100f, kept, 0.01f)
    }

    @Test
    fun `reveal pan pulls a field above the viewport down and stays clamped`() {
        // A field at the very top of the fit-width page (-250..-230) moves down, but no
        // further than the page's 250px pan room.
        val offset =
            revealOffsetY(
                currentOffsetY = 0f,
                targetTop = -250f,
                targetBottom = -230f,
                scale = 1f,
                viewportHeight = 300f,
                scaledPageHeight = 800f,
                topMargin = 16f,
                bottomMargin = 16f,
            )
        assertEquals(250f, offset, 0.01f)
    }

    @Test
    fun `reveal pan keeps the first line of an overly tall field readable`() {
        val offset =
            revealOffsetY(
                currentOffsetY = 0f,
                targetTop = 0f,
                targetBottom = 400f,
                scale = 1f,
                viewportHeight = 300f,
                scaledPageHeight = 800f,
                topMargin = 16f,
                bottomMargin = 16f,
            )
        assertEquals(16f, offset, 0.01f)
    }

    @Test
    fun `reveal pan ignores non-finite input`() {
        assertEquals(
            42f,
            revealOffsetY(42f, Float.NaN, 0f, 1f, 300f, 800f, 16f, 16f),
        )
        assertEquals(
            42f,
            revealOffsetY(42f, 0f, 10f, 0f, 300f, 800f, 16f, 16f),
        )
    }
}
