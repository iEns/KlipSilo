package com.phaprodevelop.klipsilo.data.model

import com.phaprodevelop.klipsilo.data.db.ContentType

data class ClipboardEntry(
    val id: Long,
    val contentType: ContentType,
    val textContent: String? = null,
    val filePath: String? = null,
    val previewText: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
