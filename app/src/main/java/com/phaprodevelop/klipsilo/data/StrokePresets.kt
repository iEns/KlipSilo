package com.phaprodevelop.klipsilo.data

internal val STROKE_PRESETS = floatArrayOf(0.002f, 0.005f, 0.01f, 0.018f)

internal fun nearestStrokePreset(width: Float): Float = STROKE_PRESETS.minBy { kotlin.math.abs(it - width) }

internal val MARKUP_WIDTHS = floatArrayOf(0.003f, 0.008f, 0.016f)

internal val MARKUP_TEXT_SIZES = floatArrayOf(0.014f, 0.022f, 0.034f)

internal fun nearestMarkupWidth(width: Float): Float = MARKUP_WIDTHS.minBy { kotlin.math.abs(it - width) }

internal fun nearestMarkupTextSize(size: Float): Float = MARKUP_TEXT_SIZES.minBy { kotlin.math.abs(it - size) }
