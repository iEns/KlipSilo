package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipFilterTest {
    @Test
    fun all_blankQuery_preservesListAndOrder() {
        // Arrange
        val clips =
            listOf(
                entry(id = 1, contentType = ContentType.URL, previewText = "https://a.example"),
                entry(id = 2, contentType = ContentType.TEXT, previewText = "hello"),
                entry(id = 3, contentType = ContentType.IMAGE, previewText = "photo", isLocked = true),
            )

        // Act
        val result = filterClips(clips, query = "", filter = ClipFilter.ALL)

        // Assert
        assertEquals(clips, result)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun text_excludesUrl() {
        // Arrange
        val text = entry(id = 1, contentType = ContentType.TEXT, previewText = "note")
        val url = entry(id = 2, contentType = ContentType.URL, previewText = "https://example.com")
        val clips = listOf(text, url)

        // Act
        val result = filterClips(clips, query = "", filter = ClipFilter.TEXT)

        // Assert
        assertEquals(listOf(text), result)
    }

    @Test
    fun locked_onlyIsLocked() {
        // Arrange
        val lockedText = entry(id = 1, contentType = ContentType.TEXT, previewText = "secret", isLocked = true)
        val unlockedText = entry(id = 2, contentType = ContentType.TEXT, previewText = "open")
        val lockedUrl = entry(id = 3, contentType = ContentType.URL, previewText = "https://locked", isLocked = true)
        val clips = listOf(lockedText, unlockedText, lockedUrl)

        // Act
        val result = filterClips(clips, query = "", filter = ClipFilter.LOCKED)

        // Assert
        assertEquals(listOf(lockedText, lockedUrl), result)
        assertTrue(result.all { it.isLocked })
    }

    @Test
    fun query_foo_matchesPreviewFooBar_ignoreCase() {
        // Arrange
        val match = entry(id = 1, contentType = ContentType.TEXT, previewText = "FooBar")
        val miss = entry(id = 2, contentType = ContentType.TEXT, previewText = "Baz")
        val clips = listOf(match, miss)

        // Act
        val result = filterClips(clips, query = "foo", filter = ClipFilter.ALL)

        // Assert
        assertEquals(listOf(match), result)
    }

    @Test
    fun query_doesNotSearchTextContent() {
        // Arrange
        val clips =
            listOf(
                entry(
                    id = 1,
                    contentType = ContentType.TEXT,
                    previewText = "note",
                    textContent = "foo",
                ),
            )

        // Act
        val result = filterClips(clips, query = "foo", filter = ClipFilter.ALL)

        // Assert
        assertEquals(emptyList<ClipboardEntry>(), result)
    }

    @Test
    fun contentTypeFilters_matchOnlyThatType() {
        val clips = allContentTypes()
        val expected =
            mapOf(
                ClipFilter.IMAGE to ContentType.IMAGE,
                ClipFilter.PDF to ContentType.PDF,
                ClipFilter.FILE to ContentType.FILE,
                ClipFilter.LOCATION to ContentType.LOCATION,
                ClipFilter.URL to ContentType.URL,
            )

        expected.forEach { (filter, contentType) ->
            // Act
            val result = filterClips(clips, query = "", filter = filter)

            // Assert
            assertEquals("filter $filter", listOf(contentType), result.map { it.contentType })
        }
    }

    @Test
    fun blankQuery_doesNotFilterByText() {
        // Arrange
        val clips =
            listOf(
                entry(id = 1, contentType = ContentType.TEXT, previewText = "alpha"),
                entry(id = 2, contentType = ContentType.TEXT, previewText = "beta"),
                entry(id = 3, contentType = ContentType.URL, previewText = "https://gamma"),
            )

        // Act
        val emptyQuery = filterClips(clips, query = "", filter = ClipFilter.ALL)
        val whitespaceQuery = filterClips(clips, query = "   ", filter = ClipFilter.ALL)
        val textFilter = filterClips(clips, query = "", filter = ClipFilter.TEXT)

        // Assert
        assertEquals(clips, emptyQuery)
        assertEquals(clips, whitespaceQuery)
        assertEquals(listOf(1L, 2L), textFilter.map { it.id })
    }

    @Test
    fun query_withSoftHyphenStillMatchesAndSoftHyphenPreviewStillMatches() {
        // #62: soft hyphens (U+00AD) must never break matching — not in the
        // query and not in the preview text.
        val softPreview = entry(id = 1, contentType = ContentType.TEXT, previewText = "Aus\u00ADgabe\u00ADresolution")
        assertEquals(
            listOf(1L),
            filterClips(listOf(softPreview), query = "ausgabe", filter = ClipFilter.ALL).map { it.id },
        )
        assertEquals(
            listOf(
                1L,
            ),
            filterClips(listOf(softPreview), query = "resolu\u00ADtion", filter = ClipFilter.ALL).map {
                it.id
            },
        )
        val plain = entry(id = 2, contentType = ContentType.TEXT, previewText = "Ausgaberesolution")
        assertEquals(
            listOf(2L),
            filterClips(listOf(plain), query = "reso\u00ADlution", filter = ClipFilter.ALL).map { it.id },
        )
    }

    private fun allContentTypes(): List<ClipboardEntry> =
        listOf(
            entry(id = 1, contentType = ContentType.TEXT, previewText = "text"),
            entry(id = 2, contentType = ContentType.URL, previewText = "https://example.com"),
            entry(id = 3, contentType = ContentType.IMAGE, previewText = "image"),
            entry(id = 4, contentType = ContentType.PDF, previewText = "pdf"),
            entry(id = 5, contentType = ContentType.FILE, previewText = "file"),
            entry(id = 6, contentType = ContentType.LOCATION, previewText = "location"),
        )

    private fun entry(
        id: Long,
        contentType: ContentType,
        previewText: String,
        timestamp: Long = id,
        textContent: String? = null,
        isLocked: Boolean = false,
    ): ClipboardEntry =
        ClipboardEntry(
            id = id,
            contentType = contentType,
            previewText = previewText,
            timestamp = timestamp,
            textContent = textContent,
            isLocked = isLocked,
        )
}
