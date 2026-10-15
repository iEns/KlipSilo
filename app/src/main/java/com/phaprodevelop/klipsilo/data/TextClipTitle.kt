package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.db.ContentType

internal const val TEXT_CLIP_TITLE_MAX = 60

private val headingLine = Regex("""^#{1,6}\s+\S""")
private val leadingHeading = Regex("""^#{1,6}\s*""")
private val leadingQuote = Regex("""^>\s*""")
private val leadingList = Regex("""^(?:[-*+]|\d+[.)])\s+""")
private val leadingTask = Regex("""^\[[ xX]\]\s*""")
private val emphasis = Regex("""[*_]+""")

/**
 * Title for a text clip: the first Markdown heading, otherwise the first
 * non-empty line, with markers removed and trimmed to [maxChars].
 */
internal fun textClipTitle(
    raw: String,
    maxChars: Int = TEXT_CLIP_TITLE_MAX,
): String {
    val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
    val source = lines.firstOrNull { headingLine.containsMatchIn(it) } ?: lines.firstOrNull() ?: return ""
    val stripped =
        source
            .replace(leadingHeading, "")
            .replace(leadingQuote, "")
            .replace(leadingList, "")
            .replace(leadingTask, "")
            .replace(emphasis, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    if (stripped.isEmpty()) return ""
    if (stripped.length <= maxChars) return stripped
    val cut = stripped.lastIndexOf(' ', maxChars)
    return if (cut >= maxChars / 2) stripped.take(cut).trimEnd() else stripped.take(maxChars).trimEnd()
}

/**
 * A stored preview that was copied from the body, not typed in Rename.
 * A short single line without a leading marker is treated as a user title.
 */
internal fun textTitleLooksAutomatic(preview: String): Boolean {
    val trimmed = preview.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed.length > TEXT_CLIP_TITLE_MAX) return true
    if ('\n' in trimmed || '\r' in trimmed) return true
    return trimmed.startsWith("#") ||
        trimmed.startsWith(">") ||
        trimmed.startsWith("- ") ||
        trimmed.startsWith("* ") ||
        trimmed.startsWith("+ ") ||
        trimmed.startsWith("[ ]") ||
        trimmed.startsWith("[x]", ignoreCase = true)
}

internal fun clipListTitle(
    type: ContentType,
    preview: String,
): String {
    if (type != ContentType.TEXT) return preview
    return textClipTitle(preview).ifBlank { preview }
}
