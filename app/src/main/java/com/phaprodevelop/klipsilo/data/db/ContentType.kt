package com.phaprodevelop.klipsilo.data.db

enum class ContentType {
    TEXT,
    MARKDOWN,
    IMAGE,
    URL,
    PDF,
    FILE,
    LOCATION,
}

/** Plain Text, Markdown and URL clips store an editable text body. */
val ContentType.isTextual: Boolean
    get() = this == ContentType.TEXT || this == ContentType.MARKDOWN || this == ContentType.URL

/** Plain Text and Markdown are the two editable text kinds (#127). */
val ContentType.isPlainOrMarkdown: Boolean
    get() = this == ContentType.TEXT || this == ContentType.MARKDOWN

/** The editable text kind toggled by "Make this Markdown" / "Make this Plain Text" (#127). */
fun ContentType.toggledTextKind(): ContentType =
    if (this == ContentType.MARKDOWN) ContentType.TEXT else ContentType.MARKDOWN
