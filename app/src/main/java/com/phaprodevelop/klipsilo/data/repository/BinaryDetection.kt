package com.phaprodevelop.klipsilo.data.repository

import java.io.File
import java.io.FileInputStream

internal fun isProbablyBinary(file: File): Boolean {
    if (!file.exists()) return true
    return try {
        val buffer = ByteArray(minOf(4096L, file.length()).toInt())
        FileInputStream(file).use { it.read(buffer) }
        var controlCount = 0
        for (b in buffer) {
            val u = b.toInt() and 0xFF
            if (u == 0) return true
            if (u in 1..8 || u == 11 || u == 12 || u in 14..31) {
                controlCount++
            }
        }
        controlCount > buffer.size * 0.1
    } catch (_: Exception) {
        true
    }
}
