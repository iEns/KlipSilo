package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.withoutSoftHyphens

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
            (
                query.isBlank() ||
                    // Soft hyphens must not break matching (#62): text that
                    // carries U+00AD still matches a query without it.
                    entry.previewText.withoutSoftHyphens().contains(
                        query.withoutSoftHyphens(),
                        ignoreCase = true,
                    )
            )
    }

/** Which copy the empty clip list shows (#97, #98). */
internal enum class EmptyListTitle {
    NO_CLIPS,
    SEARCH,
    FILTER,
}

/** Which hint accompanies [EmptyListTitle]. */
internal enum class EmptyListHint {
    NO_CLIPS,
    CLEAR_SEARCH_OR_FILTER,
    TEXT,
    URL,
    IMAGE,
    PDF,
    FILE,
    LOCATION,
    LOCKED,
}

internal data class EmptyListState(
    val title: EmptyListTitle,
    val hint: EmptyListHint,
)

/**
 * Selects the empty-list copy without Compose or resources so the precedence is
 * unit-tested: a search always names the query first, then a type/locked filter
 * names its chip even when the whole library is empty (#97, #98).
 */
internal fun emptyListState(
    listEmpty: Boolean,
    query: String,
    filter: ClipFilter,
): EmptyListState {
    val searching = query.isNotBlank()
    val title =
        when {
            searching -> EmptyListTitle.SEARCH
            filter != ClipFilter.ALL -> EmptyListTitle.FILTER
            else -> EmptyListTitle.NO_CLIPS
        }
    val hint =
        when {
            searching -> EmptyListHint.CLEAR_SEARCH_OR_FILTER
            filter != ClipFilter.ALL ->
                when (filter) {
                    ClipFilter.TEXT -> EmptyListHint.TEXT
                    ClipFilter.URL -> EmptyListHint.URL
                    ClipFilter.IMAGE -> EmptyListHint.IMAGE
                    ClipFilter.PDF -> EmptyListHint.PDF
                    ClipFilter.FILE -> EmptyListHint.FILE
                    ClipFilter.LOCATION -> EmptyListHint.LOCATION
                    ClipFilter.LOCKED -> EmptyListHint.LOCKED
                    ClipFilter.ALL -> EmptyListHint.NO_CLIPS
                }
            listEmpty -> EmptyListHint.NO_CLIPS
            else -> EmptyListHint.CLEAR_SEARCH_OR_FILTER
        }
    return EmptyListState(title = title, hint = hint)
}
