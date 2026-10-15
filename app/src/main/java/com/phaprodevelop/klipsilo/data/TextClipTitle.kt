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
 * A setext underline: a line of two or more `=` or `-` characters. Marks the
 * PREVIOUS non-blank line as a Markdown heading level 1/2.
 */
private val setextUnderline = Regex("""^[=\-]{2,}\s*""")

/**
 * Strip the common Markdown/quote/list/task markers from the start of one line.
 */
private fun stripTitleLine(source: String): String =
    source
        .trim()
        .replace(leadingHeading, "")
        .replace(leadingQuote, "")
        .replace(leadingList, "")
        .replace(leadingTask, "")
        .replace(emphasis, "")
        .replace(Regex("""\s+"""), " ")
        .trim()

/**
 * The first non-empty line of a string, without the rest. Keeps the row and
 * reader titles safe to render on one line next to the icons.
 */
internal fun firstTitleLine(raw: String): String {
    for (line in raw.lineSequence()) {
        if (line.isNotBlank()) return line.trim()
    }
    return ""
}

/**
 * Title for a text clip: the first Markdown heading — setext (`Title\n====`)
 * or ATX (`# Title`) — otherwise the first non-empty line, with markers
 * removed and trimmed to [maxChars]. Never returns more than one line.
 */
internal fun textClipTitle(
    raw: String,
    maxChars: Int = TEXT_CLIP_TITLE_MAX,
): String {
    val lines = raw.lineSequence().map { it.trim() }.toList()

    var fallback = ""
    for (index in lines.indices) {
        val line = lines[index]
        if (line.isEmpty()) continue
        if (headingLine.containsMatchIn(line)) {
            // An ATX heading wins when it comes before later titles.
            val stripped = stripTitleLine(line)
            if (stripped.isNotEmpty()) return truncateTitle(stripped, maxChars)
        } else if (setextUnderline.matches(line)) {
            // "Title" on the previous non-blank line, underlined below it;
            // step over marker-only lines directly above the underline.
            var probe = index - 1
            while (probe >= 0 && lines[probe].isNotEmpty() && stripTitleLine(lines[probe]).isEmpty()) probe--
            val previous = if (probe >= 0) lines[probe] else null
            if (!previous.isNullOrBlank() && !setextUnderline.matches(previous)) {
                val stripped = stripTitleLine(previous)
                if (stripped.isNotEmpty()) return truncateTitle(stripped, maxChars)
            }
        } else if (fallback.isEmpty()) {
            fallback = stripTitleLine(line)
        }
    }
    // No heading at all: the first non-empty line titles the clip.
    if (fallback.isNotEmpty()) return truncateTitle(fallback, maxChars)
    for (line in lines) {
        if (line.isEmpty()) continue
        val stripped = stripTitleLine(line)
        if (stripped.isNotEmpty()) return truncateTitle(stripped, maxChars)
    }
    return ""
}

/**
 * Word-boundary friendly cut at about [maxChars] characters.
 */
internal fun truncateTitle(
    stripped: String,
    maxChars: Int,
): String {
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

/**
 * Title shown in the clip list, the action sheet and the reader: one line,
 * never longer than the cap, Markdown markers removed, safe next to icons.
 */
internal fun clipListTitle(
    type: ContentType,
    preview: String,
): String {
    if (type != ContentType.TEXT) return firstTitleLine(preview)
    val derived = textClipTitle(preview)
    if (derived.isNotBlank()) return derived
    // Nothing derivable (e.g. a stored raw preview): fall back to its first line.
    return truncateTitle(firstTitleLine(preview), TEXT_CLIP_TITLE_MAX)
}
