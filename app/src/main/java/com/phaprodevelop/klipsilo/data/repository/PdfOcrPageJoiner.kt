package com.phaprodevelop.klipsilo.data.repository

/**
 * Joins per-page OCR results into one text body. Multi-page documents get a
 * localized page marker in front of every page that produced text; blank
 * pages are skipped. Kept pure so the joining rule is unit-testable without
 * Android or ML Kit.
 */
internal fun joinOcrPageTexts(
    pageTexts: List<String?>,
    pageSeparator: (pageNumber: Int) -> String,
): String? {
    val parts =
        pageTexts.mapIndexedNotNull { index, text ->
            if (text.isNullOrBlank()) {
                null
            } else if (pageTexts.size > 1) {
                "${pageSeparator(index + 1)}\n$text"
            } else {
                text
            }
        }
    return if (parts.isEmpty()) null else parts.joinToString("\n\n")
}
