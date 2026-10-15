package com.phaprodevelop.klipsilo.data

internal val STROKE_PRESETS = floatArrayOf(0.002f, 0.005f, 0.01f, 0.018f)

internal fun nearestStrokePreset(width: Float): Float = STROKE_PRESETS.minBy { kotlin.math.abs(it - width) }
