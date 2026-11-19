package com.phaprodevelop.klipsilo.data.repository

/** Longest display name the save dialog suggests, matching [sanitizeFileName]'s cap. */
internal const val MAX_ANNOTATED_CLIP_NAME_LENGTH = 100

/**
 * #188: suggested name for an annotated copy, e.g. "Report - annotated".
 * Strips a trailing file extension, never appends [suffix] twice, falls back
 * to [untitledName] for a blank original and keeps the suffix intact when the
 * name has to be shortened.
 */
internal fun annotatedClipName(
    originalName: String,
    suffix: String,
    untitledName: String,
): String {
    val suffixToAppend = suffix.trimEnd()
    val base = extractBaseName(originalName).trim().ifBlank { untitledName }
    if (suffixToAppend.isBlank()) return base.take(MAX_ANNOTATED_CLIP_NAME_LENGTH)
    if (base.endsWith(suffixToAppend, ignoreCase = true)) {
        return if (base.length <= MAX_ANNOTATED_CLIP_NAME_LENGTH) {
            base
        } else {
            shortenKeepingSuffix(base, suffixToAppend)
        }
    }
    val maxStemLength = (MAX_ANNOTATED_CLIP_NAME_LENGTH - suffixToAppend.length).coerceAtLeast(1)
    return base.take(maxStemLength).trimEnd() + suffixToAppend
}

private fun shortenKeepingSuffix(
    name: String,
    suffix: String,
): String {
    val maxStemLength = (MAX_ANNOTATED_CLIP_NAME_LENGTH - suffix.length).coerceAtLeast(1)
    return name.dropLast(suffix.length).take(maxStemLength).trimEnd() + suffix
}
