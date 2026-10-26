package com.phaprodevelop.klipsilo.ui.components

import kotlin.math.roundToInt

/** Smallest allowed preview zoom. */
const val PREVIEW_MIN_SCALE = 1f

/** Largest allowed preview zoom. */
const val PREVIEW_MAX_SCALE = 5f

/** Zoom applied by a double tap. */
const val PREVIEW_DOUBLE_TAP_SCALE = 2.5f

/** Base render length of the preview bitmap; scaled up while zoomed to stay sharp. */
const val PREVIEW_BASE_RENDER_DIM = 1800

/** Upper bound for the zoomed render so a sharp preview never blows the memory budget. */
const val PREVIEW_MAX_RENDER_DIM = 3200

/** Largest zoom the View Content PDF viewer allows while pinching. */
const val PDF_VIEW_MAX_SCALE = 8f

/**
 * State of the zoomable form preview.
 *
 * [scale] is the zoom factor; [offsetX]/[offsetY] are translations (px) of the page center
 * relative to the viewport center, so (0, 0) means the page is centered.
 */
data class PreviewZoomState(
    val scale: Float = PREVIEW_MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

fun clampScale(
    scale: Float,
    maxScale: Float = PREVIEW_MAX_SCALE,
): Float =
    when {
        scale.isNaN() || scale <= PREVIEW_MIN_SCALE -> PREVIEW_MIN_SCALE
        scale >= maxScale -> maxScale
        else -> scale
    }

/**
 * Clamps one axis of the pan offset so the scaled page cannot leave the viewport.
 * When the scaled page is smaller than the viewport in that axis, the page stays centered.
 * A non-finite (NaN/Infinity) input is treated as "no pan" so a bad gesture sample can never
 * stick the page off screen.
 */
fun clampOffset(
    offset: Float,
    scaledSize: Float,
    viewportSize: Float,
): Float {
    if (!offset.isFinite() || !scaledSize.isFinite() || !viewportSize.isFinite()) return 0f
    if (scaledSize <= viewportSize) return 0f
    val max = (scaledSize - viewportSize) / 2f
    return offset.coerceIn(-max, max)
}

/**
 * Clamps pan offsets for a page drawn with ContentScale.Fit inside the viewport.
 *
 * [pageWidth] x [pageHeight] are the page's unscaled (bitmap) dimensions. The page is first
 * fitted into the viewport (letterboxed on the shorter axis), then scaled by [scale]. Offsets
 * are relative to the viewport center, like [PreviewZoomState]. When the scaled page is smaller
 * than the viewport on an axis, it stays centered on that axis. Pure; unit tested.
 */
fun clampFittedPan(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
): PreviewZoomState {
    // A non-finite scale/offset can only come from a wild gesture sample. Reset to the
    // centred fit state instead of propagating NaN into graphicsLayer (which draws nothing).
    if (!scale.isFinite() || !offsetX.isFinite() || !offsetY.isFinite() || scale <= 0f) {
        return PreviewZoomState()
    }
    if (
        viewportWidth <= 0f ||
        viewportHeight <= 0f ||
        pageWidth <= 0f ||
        pageHeight <= 0f ||
        !viewportWidth.isFinite() ||
        !viewportHeight.isFinite() ||
        !pageWidth.isFinite() ||
        !pageHeight.isFinite()
    ) {
        return PreviewZoomState(scale = scale, offsetX = offsetX, offsetY = offsetY)
    }
    val fit = minOf(viewportWidth / pageWidth, viewportHeight / pageHeight)
    return PreviewZoomState(
        scale = scale,
        offsetX = clampOffset(offsetX, pageWidth * fit * scale, viewportWidth),
        offsetY = clampOffset(offsetY, pageHeight * fit * scale, viewportHeight),
    )
}

/**
 * Offset after scaling by [factor] (new scale divided by old scale) while keeping the viewport
 * point [focal] (relative to the viewport center) stationary on screen.
 */
fun zoomedOffset(
    offset: Float,
    factor: Float,
    focal: Float,
): Float = offset * factor + focal * (1f - factor)

/**
 * Applies one pinch or pan step: zoom by [zoomDelta] around the viewport point
 * ([focalX], [focalY]) and pan by ([panX], [panY]) screen px, then clamps so the page
 * cannot leave the [viewportWidth] x [viewportHeight] viewport. Pure; unit tested.
 */
fun transformPreviewZoom(
    current: PreviewZoomState,
    zoomDelta: Float,
    panX: Float,
    panY: Float,
    focalX: Float,
    focalY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
): PreviewZoomState {
    val safeDelta = if (zoomDelta <= 0f || zoomDelta.isNaN()) 1f else zoomDelta
    val scale = clampScale(current.scale * safeDelta)
    val factor = if (current.scale <= 0f) 1f else scale / current.scale
    return PreviewZoomState(
        scale = scale,
        offsetX =
            clampOffset(
                zoomedOffset(current.offsetX, factor, focalX) + panX,
                pageWidth * scale,
                viewportWidth,
            ),
        offsetY =
            clampOffset(
                zoomedOffset(current.offsetY, factor, focalY) + panY,
                pageHeight * scale,
                viewportHeight,
            ),
    )
}

/**
 * One pinch or pan step for a page drawn with ContentScale.Fit (#148).
 *
 * Zooms by [zoomDelta] around the viewport point ([focalX], [focalY]) and pans by
 * ([panX], [panY]) screen px. [pageWidth] x [pageHeight] are the unscaled page dimensions;
 * the pan bounds use the fitted page size via [clampFittedPan]. Zooming around the focal
 * point keeps the content under the fingers in place instead of throwing it off screen.
 * Non-finite input is ignored so a wild gesture sample cannot blank the page. Pure; unit tested.
 */
fun transformFittedZoom(
    current: PreviewZoomState,
    zoomDelta: Float,
    panX: Float,
    panY: Float,
    focalX: Float,
    focalY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
    maxScale: Float = PREVIEW_MAX_SCALE,
): PreviewZoomState {
    if (!current.scale.isFinite() || current.scale <= 0f) return PreviewZoomState()
    val safeDelta =
        if (!zoomDelta.isFinite() || zoomDelta <= 0f) 1f else zoomDelta
    val nextScale = clampScale(current.scale * safeDelta, maxScale)
    val factor = nextScale / current.scale
    return clampFittedPan(
        offsetX = zoomedOffset(current.offsetX, factor, focalX) + panX,
        offsetY = zoomedOffset(current.offsetY, factor, focalY) + panY,
        scale = nextScale,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        pageWidth = pageWidth,
        pageHeight = pageHeight,
    )
}

/**
 * Double tap: already zoomed resets to 1x centered; at 1x zooms to [PREVIEW_DOUBLE_TAP_SCALE]
 * keeping the tapped page point ([focalX], [focalY], viewport relative) under the finger.
 */
fun doubleTapPreviewZoom(
    current: PreviewZoomState,
    focalX: Float,
    focalY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
): PreviewZoomState {
    if (current.scale > PREVIEW_MIN_SCALE) {
        return PreviewZoomState()
    }
    val factor = PREVIEW_DOUBLE_TAP_SCALE / current.scale
    return PreviewZoomState(
        scale = PREVIEW_DOUBLE_TAP_SCALE,
        offsetX =
            clampOffset(
                zoomedOffset(current.offsetX, factor, focalX),
                pageWidth * PREVIEW_DOUBLE_TAP_SCALE,
                viewportWidth,
            ),
        offsetY =
            clampOffset(
                zoomedOffset(current.offsetY, factor, focalY),
                pageHeight * PREVIEW_DOUBLE_TAP_SCALE,
                viewportHeight,
            ),
    )
}

/** Re-clamps offsets after the viewport size changed (e.g. the preview collapsed for the IME). */
fun clampPreviewZoom(
    current: PreviewZoomState,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
): PreviewZoomState =
    current.copy(
        offsetX = clampOffset(current.offsetX, pageWidth * current.scale, viewportWidth),
        offsetY = clampOffset(current.offsetY, pageHeight * current.scale, viewportHeight),
    )

/**
 * Maps a point in preview coordinates (px, relative to the viewport top-left) to page-local
 * coordinates (px, relative to the page top-left) under the given zoom state. Pure; unit tested.
 */
fun previewPointToPageLocal(
    zoom: PreviewZoomState,
    viewportX: Float,
    viewportY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pageWidth: Float,
    pageHeight: Float,
): Pair<Float, Float> =
    Pair(
        (viewportX - viewportWidth / 2f - zoom.offsetX) / zoom.scale + pageWidth / 2f,
        (viewportY - viewportHeight / 2f - zoom.offsetY) / zoom.scale + pageHeight / 2f,
    )

/**
 * Render length to request for the current zoom, or null when the current bitmap is still
 * sharp enough (small zoom differences do not justify re-rendering). Pure; unit tested.
 */
fun previewRenderDim(
    currentDim: Int,
    scale: Float,
): Int? {
    val wanted =
        (PREVIEW_BASE_RENDER_DIM * scale.coerceAtLeast(PREVIEW_MIN_SCALE))
            .roundToInt()
            .coerceAtMost(PREVIEW_MAX_RENDER_DIM)
    if (currentDim == wanted) return null
    val ratio = wanted.toFloat() / currentDim.toFloat()
    return if (ratio >= 1.2f || ratio <= 0.8f) wanted else null
}
