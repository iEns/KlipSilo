package com.phaprodevelop.klipsilo

/**
 * The soft hyphen U+00AD (#62). Grok Build inserts it into long localized
 * DE/DA strings as a hyphenation hint for the fit-text rule. Inside a
 * visible label the rule draws it as a hyphen where the line breaks; anywhere
 * else it must never leak — TalkBack would read it, a clipboard, a search
 * index, a file name or a PDF would carry or render it — so every use of a
 * localized string outside a visible label goes through
 * [withoutInvisibleBreaks].
 */
const val CHAR_SOFT_HYPHEN = '\u00AD'

/**
 * The zero-width space U+200B (#64). Like [CHAR_SOFT_HYPHEN] it is a
 * display-only break opportunity (#54: after `_` `-` `.` `/`, the version
 * line after `+`/`-`) that must never reach contentDescription or text
 * semantics.
 */
const val CHAR_ZERO_WIDTH_SPACE = '\u200B'

/**
 * [this] with every invisible break character — the soft hyphen U+00AD and
 * the zero-width space U+200B — removed: for strings used outside a visible
 * label — contentDescription/semantics, copy/share, search matching, file
 * names, PDF output (#62, #64).
 */
fun String.withoutInvisibleBreaks(): String =
    if (contains(CHAR_SOFT_HYPHEN) || contains(CHAR_ZERO_WIDTH_SPACE)) {
        replace(CHAR_SOFT_HYPHEN.toString(), "").replace(CHAR_ZERO_WIDTH_SPACE.toString(), "")
    } else {
        this
    }

/**
 * [this] with every soft hyphen U+00AD removed: for strings used outside a
 * visible label — contentDescription/semantics, copy/share, search
 * matching, file names, PDF output (#62). Also strips the zero-width space
 * U+200B (#64): both marks are display-only, so no consumer outside a
 * visible label may see either.
 */
fun String.withoutSoftHyphens(): String = withoutInvisibleBreaks()
