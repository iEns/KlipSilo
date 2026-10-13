package com.phaprodevelop.klipsilo.data.db

/** List projection for [ClipboardItem] that omits [ClipboardItem.textContent]. */
data class ClipboardItemListRow(
    val id: Long,
    val contentType: ContentType,
    val filePath: String? = null,
    val previewText: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
