package com.phaprodevelop.klipsilo.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

private const val MAX_RENDER_DIM = 4096

fun renderPdfPage(
    filePath: String,
    maxDimension: Int,
    pageIndex: Int = 0,
): Bitmap? {
    return try {
        val file = File(filePath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val renderer = PdfRenderer(pfd)
            try {
                if (renderer.pageCount == 0) return null
                val index = pageIndex.coerceIn(0, renderer.pageCount - 1)
                val page = renderer.openPage(index)
                try {
                    val effectiveMaxDim = minOf(maxDimension, MAX_RENDER_DIM)
                    val scale =
                        minOf(
                            effectiveMaxDim.toFloat() / page.width.toFloat(),
                            effectiveMaxDim.toFloat() / page.height.toFloat(),
                        )
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    bitmap
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        } finally {
            pfd.close()
        }
    } catch (_: Exception) {
        null
    }
}

fun pdfPageCount(filePath: String): Int {
    return try {
        val file = File(filePath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val renderer = PdfRenderer(pfd)
            try {
                renderer.pageCount
            } finally {
                renderer.close()
            }
        } finally {
            pfd.close()
        }
    } catch (_: Exception) {
        0
    }
}

fun pdfPageHeightPt(
    filePath: String,
    pageIndex: Int = 0,
): Float {
    return try {
        val file = File(filePath)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val renderer = PdfRenderer(pfd)
            try {
                val index = pageIndex.coerceIn(0, renderer.pageCount - 1)
                val page = renderer.openPage(index)
                try {
                    page.height.toFloat()
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        } finally {
            pfd.close()
        }
    } catch (_: Exception) {
        0f
    }
}
