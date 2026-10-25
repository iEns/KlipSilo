package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlClipPaneTest {
    private val viewportMeta = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
    private val styleBlock =
        "<style>body{margin:0;padding:16px;font-size:1rem;line-height:1.45;-webkit-text-size-adjust:100%}</style>"
    private val injection = viewportMeta + styleBlock

    @Test
    fun `detects html document by doctype and html tag`() {
        assertTrue(looksLikeHtmlDocument("<!DOCTYPE html><html><body>hi</body></html>"))
        assertTrue(looksLikeHtmlDocument("<html>\n<head><meta charset=\"utf-8\"></head></html>"))
        assertTrue(looksLikeHtmlDocument("  <HTML><BODY>hest</BODY></HTML>"))
    }

    @Test
    fun `detects body fragment`() {
        assertTrue(looksLikeHtmlDocument("<body><p>fragment</p></body>"))
    }

    @Test
    fun `plain text is not an html document`() {
        assertFalse(looksLikeHtmlDocument(null))
        assertFalse(looksLikeHtmlDocument(""))
        assertFalse(looksLikeHtmlDocument("   "))
        assertFalse(looksLikeHtmlDocument("Weekend i København\n\nIngredienser: ..."))
        assertFalse(looksLikeHtmlDocument("< let's meet at <corner> and a \"bodied\" note"))
    }

    @Test
    fun `injects reader style into document head`() {
        val source = "<html>\n<head>\n<meta charset=\"utf-8\">\n</head>\n<body><p>Lørdag</p></body>\n</html>\n"
        val head = source.substringBefore("</head>")
        assertEquals(
            head + injection + "</head>\n<body><p>Lørdag</p></body>\n</html>\n",
            renderableClipHtml(source),
        )
    }

    @Test
    fun `adds a head to a document without one`() {
        val rendered = renderableClipHtml("<html><body><p>hallo</p></body></html>")
        assertEquals("<html><head>" + injection + "</head><body><p>hallo</p></body></html>", rendered)
    }

    @Test
    fun `wraps bare fragments in a document`() {
        assertEquals(
            "<!doctype html><html><head>" + injection + "</head><body><p>some notes</p></body></html>",
            renderableClipHtml("<p>some notes</p>"),
        )
    }

    @Test
    fun `handles uppercase head tags`() {
        val source = "<HTML><HEAD><meta charset=\"utf-8\"></HEAD><BODY><p>hi</p></BODY></HTML>"
        assertEquals(
            "<HTML><HEAD><meta charset=\"utf-8\">" + injection + "</HEAD><BODY><p>hi</p></BODY></HTML>",
            renderableClipHtml(source),
        )
    }

    @Test
    fun `handles head tag without close`() {
        val rendered = renderableClipHtml("<html><head><meta charset=\"utf-8\"><body></html>")
        assertEquals("<html><head>" + injection + "<meta charset=\"utf-8\"><body></html>", rendered)
    }
}
