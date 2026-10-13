package com.phaprodevelop.klipsilo.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey
    val id: Long,
    val contentType: ContentType,
    val textContent: String? = null,
    val filePath: String? = null,
    val previewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
