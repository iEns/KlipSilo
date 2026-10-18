package com.phaprodevelop.klipsilo.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface

/**
 * Upper bound on decoded pixels (16 MP ≈ 64 MB ARGB_8888): stays under the
 * 100 MB draw limit with zoom headroom.
 */
internal const val MAX_IMAGE_DECODE_PIXELS = 16L * 1024L * 1024L

/**
 * The power-of-two [inSampleSize] for decoding a `srcWidth × srcHeight`
 * image into a `dstWidth × dstHeight` view, fitted like Coil's Scale.FIT
 * (the larger of the per-dimension samples), then doubled until the decoded
 * pixel count fits [maxDecodePixels]. Never zero and never negative: a
 * corrupt file (0 × 0) samples at 1 and the caller rejects the decode.
 */
internal fun imageSampleSize(
    srcWidth: Int,
    srcHeight: Int,
    dstWidth: Int,
    dstHeight: Int,
    maxDecodePixels: Long = MAX_IMAGE_DECODE_PIXELS,
): Int {
    if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) return 1
    val widthSample = Integer.highestOneBit(maxOf(1, srcWidth / dstWidth))
    val heightSample = Integer.highestOneBit(maxOf(1, srcHeight / dstHeight))
    var sample = maxOf(widthSample, heightSample).coerceAtLeast(1)
    while (
        srcWidth / sample.toLong() * (srcHeight / sample.toLong()) > maxDecodePixels &&
        sample < 1 shl 20
    ) {
        sample *= 2
    }
    return sample
}

/**
 * Decoded bitmap for a view of [dstWidthPx] × [dstHeightPx], bounded by
 * [maxDecodePixels] and EXIF-corrected, or null when the file is missing,
 * not decodable, or the decode fails — including OutOfMemoryError, which
 * BitmapFactory can throw for pathological files.
 */
internal fun decodeImageBitmap(
    filePath: String,
    dstWidthPx: Int,
    dstHeightPx: Int,
    maxDecodePixels: Long = MAX_IMAGE_DECODE_PIXELS,
): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample =
            imageSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                dstWidthPx,
                dstHeightPx,
                maxDecodePixels,
            )
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(filePath, options) ?: return null
        exifRotate(decoded, filePath)
    } catch (_: Throwable) {
        // Throwable incl. OutOfMemoryError (#66): fall back to the info
        // card, never crash.
        null
    }
}

/** Applies the file's EXIF orientation; falls back to the input bitmap. */
internal fun exifRotate(
    bitmap: Bitmap,
    filePath: String,
): Bitmap {
    return try {
        val exif = ExifInterface(filePath)
        val orientation =
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                matrix.postTranslate(bitmap.width.toFloat(), 0f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
                matrix.postTranslate(0f, bitmap.height.toFloat())
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(bitmap.height.toFloat(), 0f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(1f, -1f)
                matrix.postTranslate(0f, bitmap.width.toFloat())
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            try {
                bitmap.recycle()
            } catch (_: Exception) {
            }
        }
        rotated
    } catch (_: Exception) {
        bitmap
    } catch (_: OutOfMemoryError) {
        bitmap
    }
}
