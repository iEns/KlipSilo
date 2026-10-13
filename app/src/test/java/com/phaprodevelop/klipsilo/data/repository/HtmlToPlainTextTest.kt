package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlToPlainTextTest {
    @Test
    fun htmlToPlainText_stripsParagraphTags() {
        // Arrange
        val html = "<p>Hello</p>"

        // Act
        val result = htmlToPlainText(html)

        // Assert
        assertEquals("Hello", result)
    }

    @Test
    fun htmlToPlainText_brBecomesNewlines() {
        // Arrange
        val html = "a<br>b<br/>c"

        // Act
        val result = htmlToPlainText(html)

        // Assert
        assertEquals("a\nb\nc", result)
    }

    @Test
    fun htmlToPlainText_decodesEntities() {
        // Arrange: trailing &nbsp; becomes a space, then line trim drops it
        val html = "&amp; &lt; &gt; &quot; &#39; &nbsp;"

        // Act
        val result = htmlToPlainText(html)

        // Assert
        assertEquals("& < > \" '", result)
    }

    @Test
    fun htmlToPlainText_imgDoesNotLeakAttributes() {
        // Arrange
        val html = "<img src='x'>"

        // Act
        val result = htmlToPlainText(html)

        // Assert: img regex removes the whole tag; nothing visible remains
        assertEquals("", result)
        assertFalse(result.contains("src"))
        assertFalse(result.contains("x"))
    }

    @Test
    fun htmlToPlainText_stripsInlineMarkup() {
        // Arrange
        val html = "<b>Bold</b> and <i>i</i>"

        // Act
        val result = htmlToPlainText(html)

        // Assert
        assertEquals("Bold and i", result)
    }

    @Test
    fun htmlToPlainText_collapsesConsecutiveBlankLines() {
        // Arrange: two empty paragraphs between content
        val html = "<p>a</p><p></p><p></p><p>b</p>"

        // Act
        val result = htmlToPlainText(html)

        // Assert: fold keeps a single blank line, then outer trim
        assertEquals("a\n\nb", result)
        assertFalse(result.contains("\n\n\n"))
    }

    @Test
    fun htmlToPlainText_truncatesAtMaxPreviewChars() {
        // Arrange
        val html = "<p>" + "x".repeat(500_001) + "</p>"

        // Act
        val result = htmlToPlainText(html)

        // Assert
        assertTrue(result.endsWith("\n…"))
        val beforeEllipsis = result.removeSuffix("\n…")
        assertEquals(500_000, beforeEllipsis.length)
        assertEquals("x".repeat(500_000), beforeEllipsis)
        assertEquals(500_002, result.length)
    }
}
