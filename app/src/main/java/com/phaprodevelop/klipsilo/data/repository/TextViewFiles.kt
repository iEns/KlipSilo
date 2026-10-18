package com.phaprodevelop.klipsilo.data.repository

import java.io.File

/**
 * Display cap for extracted/plain text, the same limit the office
 * extractors apply ([ClipboardRepository.htmlToPlainText]). A Text
 * composable with megabytes of raw text blows up the layout pass, so the
 * viewer shows the first chunk.
 */
internal const val MAX_TEXT_VIEW_CHARS = 500_000

/** Extracted text for the file viewer: null when the file has no usable text. */
internal fun textViewFileText(filePath: String): String? {
    return when (filePath.substringAfterLast('.', "").lowercase()) {
        "docx" -> extractDocxText(filePath)
        "xlsx" -> extractXlsxText(filePath)
        "rtf" -> extractRtfText(filePath) ?: readTextViewPlain(filePath)
        else -> readTextViewPlain(filePath)
    }
}

/**
 * Plain-text extraction with the size, binary and display caps. The byte
 * cap ([MAX_TEXT_VIEW_BYTES], also used by the office/RTF extractors) keeps
 * the extraction itself bounded.
 */
internal fun readTextViewPlain(filePath: String): String? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        if (file.length() > MAX_TEXT_VIEW_BYTES) return null
        if (isProbablyBinary(file)) return null
        cappedTextViewText(file.readText())
    } catch (_: Throwable) {
        // Throwable incl. OutOfMemoryError: readText() of a huge file can
        // OOM and the viewer must fall back, not crash (#66).
        null
    }
}

/** First [MAX_TEXT_VIEW_CHARS] characters, then an ellipsis line. */
internal fun cappedTextViewText(text: String): String =
    if (text.length > MAX_TEXT_VIEW_CHARS) {
        text.take(MAX_TEXT_VIEW_CHARS) + "\n…"
    } else {
        text
    }
