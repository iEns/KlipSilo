package com.phaprodevelop.klipsilo.data

import java.util.Locale

/**
 * True when the text is an HTML document (or a body fragment) rather than plain
 * text. Shared by the file viewer and the one-shot text-kind migration (#127).
 */
internal fun looksLikeHtmlDocument(text: String?): Boolean {
    val trimmed = text.orEmpty().trimStart()
    if (trimmed.isEmpty()) return false
    val head = trimmed.take(64).lowercase(Locale.ROOT)
    if (head.startsWith("<!doctype html") || head.startsWith("<html")) return true
    return trimmed.contains("<body", ignoreCase = true)
}
