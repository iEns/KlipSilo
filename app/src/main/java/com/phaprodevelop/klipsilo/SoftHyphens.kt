package com.phaprodevelop.klipsilo

/**
 * The soft hyphen U+00AD (#62). Grok Build inserts it into long localized
 * DE/DA strings as a hyphenation hint for the fit-text rule. Inside a
 * visible label the rule draws it as a hyphen where the line breaks; anywhere
 * else it must never leak — TalkBack would read it, a clipboard, a search
 * index, a file name or a PDF would carry or render it — so every use of a
 * localized string outside a visible label goes through
 * [withoutSoftHyphens].
 */
const val CHAR_SOFT_HYPHEN = '\u00AD'

/**
 * [this] with every soft hyphen U+00AD removed: for strings used outside a
 * visible label — contentDescription/semantics, copy/share, search
 * matching, file names, PDF output (#62).
 */
fun String.withoutSoftHyphens(): String =
    if (contains(CHAR_SOFT_HYPHEN)) replace(CHAR_SOFT_HYPHEN.toString(), "") else this
