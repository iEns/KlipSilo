package com.phaprodevelop.klipsilo.ui.components

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlin.math.roundToInt

private const val READER_VIEWPORT_META = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"

/** 16dp side padding plus a readable 16sp-equivalent body size in the rendered view (#32). */
private const val READER_STYLE_BLOCK =
    "<style>body{margin:0;padding:16px;font-size:1rem;line-height:1.45;-webkit-text-size-adjust:100%}</style>"

/** True when the text is an HTML document (or a body fragment) rather than plain text. */
internal fun looksLikeHtmlDocument(text: String?): Boolean {
    val trimmed = text.orEmpty().trimStart()
    if (trimmed.isEmpty()) return false
    val head = trimmed.take(64).lowercase(Locale.ROOT)
    if (head.startsWith("<!doctype html") || head.startsWith("<html")) return true
    return trimmed.contains("<body", ignoreCase = true)
}

/**
 * The detail editor opens HTML clips as editable source again (v1.1.0
 * behaviour, #69); the rendered view is reached only through the preview
 * toggle.
 */
internal const val HTML_SOURCE_SHOWN_BY_DEFAULT = true

/** The main pane the detail editor shows for a clip (#69). */
internal enum class DetailEditorPane { TEXT_SOURCE, HTML_SOURCE, HTML_PREVIEW, TEXT_PREVIEW }

/**
 * Which main pane the detail editor shows (#69): the rendered HTML preview
 * replaces the editable source only when the preview toggle says so, and a
 * plain-text preview never hides an HTML source editor.
 */
internal fun detailEditorPane(
    isHtmlDocument: Boolean,
    htmlSourceShown: Boolean,
    showPreview: Boolean,
): DetailEditorPane =
    when {
        showPreview -> DetailEditorPane.TEXT_PREVIEW
        isHtmlDocument && !htmlSourceShown -> DetailEditorPane.HTML_PREVIEW
        isHtmlDocument -> DetailEditorPane.HTML_SOURCE
        else -> DetailEditorPane.TEXT_SOURCE
    }

/**
 * Prepares an HTML clip for the reader WebView: injects the viewport meta and the
 * reader style (padding + readable body size) into the document's head, adding a
 * head when the clip is a fragment. The rendered result matches a fresh
 * PDF-to-HTML conversion (#32).
 */
internal fun renderableClipHtml(html: String): String {
    val injection = READER_VIEWPORT_META + READER_STYLE_BLOCK
    val lower = html.lowercase(Locale.ROOT)
    val headClose = lower.indexOf("</head>")
    if (headClose >= 0) return html.take(headClose) + injection + html.substring(headClose)
    lower.indexOf("<head").let { headOpen ->
        if (headOpen >= 0) {
            val tagEnd = lower.indexOf('>', headOpen)
            if (tagEnd >= 0) return html.take(tagEnd + 1) + injection + html.substring(tagEnd + 1)
        }
    }
    lower.indexOf("<html").let { htmlOpen ->
        if (htmlOpen >= 0) {
            val tagEnd = lower.indexOf('>', htmlOpen)
            if (tagEnd >= 0) {
                return html.take(tagEnd + 1) + "<head>" + injection + "</head>" + html.substring(tagEnd + 1)
            }
        }
    }
    return "<!doctype html><html><head>" + injection + "</head><body>" + html + "</body></html>"
}

/**
 * The rendered view for HTML document clips, shared by the detail editor and the
 * presentation reader so a saved clip reopens exactly like a fresh conversion (#32).
 */
@Composable
internal fun HtmlClipPane(
    html: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        },
        update = { webView ->
            // Respect the system font scale in the rendered view (#32).
            webView.settings.textZoom = (100f * webView.resources.configuration.fontScale).roundToInt()
            if (webView.tag != html) {
                webView.loadData(renderableClipHtml(html), "text/html", null)
                webView.tag = html
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
