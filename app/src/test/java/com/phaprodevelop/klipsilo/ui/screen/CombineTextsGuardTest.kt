package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #105: Combine texts needs at least two text clips before it opens a picker. */
class CombineTextsGuardTest {
    @Test
    fun emptyLibraryCannotCombineTexts() {
        assertFalse(hasTwoTextClips(emptyList()))
    }

    @Test
    fun oneTextClipCannotCombineTexts() {
        assertFalse(hasTwoTextClips(listOf(clip(1L, ContentType.TEXT))))
    }

    @Test
    fun oneTextClipPlusAnotherTypeCannotCombineTexts() {
        assertFalse(
            hasTwoTextClips(
                listOf(
                    clip(1L, ContentType.TEXT),
                    clip(2L, ContentType.PDF),
                ),
            ),
        )
    }

    @Test
    fun twoTextClipsCanCombineTexts() {
        assertTrue(
            hasTwoTextClips(
                listOf(
                    clip(1L, ContentType.TEXT),
                    clip(2L, ContentType.TEXT),
                    clip(3L, ContentType.IMAGE),
                ),
            ),
        )
    }

    @Test
    fun plainAndMarkdownCanCombineTexts() {
        assertTrue(
            hasTwoTextClips(
                listOf(
                    clip(1L, ContentType.TEXT),
                    clip(2L, ContentType.MARKDOWN),
                ),
            ),
        )
    }

    @Test
    fun twoMarkdownClipsCanCombineTexts() {
        assertTrue(
            hasTwoTextClips(
                listOf(
                    clip(1L, ContentType.MARKDOWN),
                    clip(2L, ContentType.MARKDOWN),
                ),
            ),
        )
    }

    private fun clip(
        id: Long,
        type: ContentType,
    ) = ClipboardEntry(
        id = id,
        contentType = type,
        previewText = "clip $id",
        timestamp = 0L,
    )
}
