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
): String = suffixedClipName(originalName, suffix, untitledName)

/** #188: display name for a duplicated clip, e.g. "Report - copy". */
internal fun copiedClipName(
    originalName: String,
    copySuffix: String,
    untitledName: String,
): String = suffixedClipName(originalName, copySuffix, untitledName)

/**
 * #188: display and file stem for a single PDF page, e.g. "Quarterly Report_p3".
 * Falls back to the source file's base name when the clip preview is blank or
 * just the generic PDF label, so a shared-in PDF keeps its real name.
 */
internal fun pdfPageClipName(
    previewText: String,
    sourcePath: String?,
    pageNumber: Int,
    untitledName: String,
): String {
    val previewStem = extractBaseName(previewText).trim()
    val sourceStem = sourcePath?.let { extractBaseName(java.io.File(it).name).trim() }.orEmpty()
    val base =
        when {
            !previewStem.isGenericPdfName(untitledName) -> previewStem
            !sourceStem.isGenericPdfName(untitledName) -> sourceStem
            previewStem.isNotBlank() -> previewStem
            sourceStem.isNotBlank() -> sourceStem
            else -> untitledName
        }
    return suffixedClipName(base, "_p${pageNumber.coerceAtLeast(1)}", untitledName)
}

private fun String.isGenericPdfName(untitledName: String): Boolean =
    isBlank() || equals("pdf", ignoreCase = true) || equals(untitledName, ignoreCase = true)

/**
 * Shared naming rule for copies: strips a trailing extension, never appends
 * [suffix] twice, falls back to [untitledName] for a blank original and keeps
 * the suffix intact when the name has to be shortened.
 */
internal fun suffixedClipName(
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
