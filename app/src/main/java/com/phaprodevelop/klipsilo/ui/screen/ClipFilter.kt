package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry

internal enum class ClipFilter {
    ALL,
    TEXT,
    URL,
    IMAGE,
    PDF,
    FILE,
    LOCATION,
    LOCKED,
}

internal fun ClipFilter.matches(entry: ClipboardEntry): Boolean =
    when (this) {
        ClipFilter.ALL -> true
        ClipFilter.TEXT -> entry.contentType == ContentType.TEXT
        ClipFilter.URL -> entry.contentType == ContentType.URL
        ClipFilter.IMAGE -> entry.contentType == ContentType.IMAGE
        ClipFilter.PDF -> entry.contentType == ContentType.PDF
        ClipFilter.FILE -> entry.contentType == ContentType.FILE
        ClipFilter.LOCATION -> entry.contentType == ContentType.LOCATION
        ClipFilter.LOCKED -> entry.isLocked
    }

internal fun filterClips(
    clips: List<ClipboardEntry>,
    query: String,
    filter: ClipFilter,
): List<ClipboardEntry> =
    clips.filter { entry ->
        filter.matches(entry) &&
            (query.isBlank() || entry.previewText.contains(query, ignoreCase = true))
    }
