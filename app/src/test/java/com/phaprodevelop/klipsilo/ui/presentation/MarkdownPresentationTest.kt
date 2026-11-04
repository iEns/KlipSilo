@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.presentation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownPresentationTest {
    private fun render(markdown: String): String =
        markdownPresentationText(
            markdown = markdown,
            fontSize = 16f,
            textColor = Color.Black,
            linkColor = Color.Blue,
        ).text

    @Test
    fun bulletList_keepsEveryItemOnItsOwnLine() {
        assertEquals(
            "• one\n• two",
            render("- one\n- two"),
        )
    }

    @Test
    fun orderedList_keepsNumbersAndStartsAtTheMarkerNumber() {
        assertEquals(
            "3. three\n4. four",
            render("3. three\n4. four"),
        )
    }

    @Test
    fun listItem_withASecondParagraph_indentsItUnderTheMarker() {
        assertEquals(
            "• first paragraph\n  second paragraph",
            render("- first paragraph\n\n  second paragraph"),
        )
    }

    @Test
    fun nestedList_isIndentedUnderItsParentItem() {
        assertEquals(
            "• parent\n  • child one\n  • child two\n• next",
            render("- parent\n  - child one\n  - child two\n- next"),
        )
    }

    @Test
    fun blockQuote_keepsTheQuoteMarkerAndDropsTheExtraBlankLine() {
        assertEquals(
            "› quoted text",
            render("> quoted text"),
        )
    }

    @Test
    fun blockQuote_withTwoParagraphs_marksEveryLine() {
        assertEquals(
            "› first\n› second",
            render("> first\n>\n> second"),
        )
    }

    @Test
    fun blockQuote_withAList_marksEveryListLine() {
        assertEquals(
            "› • one\n› • two",
            render("> - one\n> - two"),
        )
    }
}
