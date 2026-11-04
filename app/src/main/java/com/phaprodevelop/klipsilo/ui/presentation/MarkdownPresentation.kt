package com.phaprodevelop.klipsilo.ui.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/**
 * #127: the reader's Markdown rendering path. Plain Text clips never pass
 * through this; their body is shown literally. The renderer keeps block
 * structure (headings, lists, quotes, code) and inline emphasis without
 * pulling in a WebView, so font size, theme and auto-scroll still work.
 */
internal fun markdownPresentationText(
    markdown: String,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
): AnnotatedString {
    val root =
        Parser.builder()
            .extensions(listOf(StrikethroughExtension.create()))
            .build()
            .parse(markdown)
    return buildAnnotatedString {
        appendMarkdownBlock(root, fontSize, textColor, linkColor)
    }.trimTrailingNewlines()
}

private fun AnnotatedString.Builder.appendMarkdownBlock(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
    compact: Boolean = false,
) {
    when (node) {
        is Heading -> {
            val factor = headingFactor(node.level)
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSize * factor).sp,
                    color = textColor,
                ),
            ) {
                appendMarkdownInlines(node, fontSize, textColor, linkColor)
            }
            append(if (compact) "\n" else "\n\n")
        }
        is Paragraph -> {
            appendMarkdownInlines(node, fontSize, textColor, linkColor)
            append(if (compact) "\n" else "\n\n")
        }
        is BlockQuote -> {
            val content =
                buildAnnotatedString {
                    appendMarkdownBlocks(node, fontSize, textColor, linkColor, compact = true)
                }.trimTrailingNewlines()
            if (content.text.isNotEmpty()) {
                appendPrefixedLines(content, "› ", "› ")
            }
            append("\n\n")
        }
        is BulletList ->
            appendList(
                node,
                ordered = false,
                fontSize = fontSize,
                textColor = textColor,
                linkColor = linkColor,
            )
        is OrderedList ->
            appendList(
                node,
                ordered = true,
                fontSize = fontSize,
                textColor = textColor,
                linkColor = linkColor,
            )
        is ListItem -> appendMarkdownBlocks(node, fontSize, textColor, linkColor, compact = true)
        is FencedCodeBlock -> appendCodeBlock(node.literal)
        is IndentedCodeBlock -> appendCodeBlock(node.literal)
        is ThematicBreak -> append("────────\n\n")
        else -> appendMarkdownBlocks(node, fontSize, textColor, linkColor, compact)
    }
}

private fun AnnotatedString.Builder.appendMarkdownBlocks(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
    compact: Boolean = false,
) {
    var child = node.firstChild
    while (child != null) {
        appendMarkdownBlock(child, fontSize, textColor, linkColor, compact)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendMarkdownInlines(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
) {
    var child = node.firstChild
    while (child != null) {
        appendMarkdownInline(child, fontSize, textColor, linkColor)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendMarkdownInline(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
) {
    when (node) {
        is StrongEmphasis ->
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendMarkdownInlines(node, fontSize, textColor, linkColor)
            }
        is Emphasis ->
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendMarkdownInlines(node, fontSize, textColor, linkColor)
            }
        is Strikethrough ->
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendMarkdownInlines(node, fontSize, textColor, linkColor)
            }
        is Code ->
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                ),
            ) {
                append(node.literal)
            }
        is Link ->
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                appendMarkdownInlines(node, fontSize, textColor, linkColor)
            }
        is Image -> append(node.title ?: node.destination.orEmpty())
        is Text -> append(node.literal)
        is SoftLineBreak -> append(" ")
        is HardLineBreak -> append("\n")
        else -> appendMarkdownInlines(node, fontSize, textColor, linkColor)
    }
}

private fun AnnotatedString.Builder.appendList(
    list: Node,
    ordered: Boolean,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
) {
    var index = (list as? OrderedList)?.markerStartNumber ?: 1
    var item = list.firstChild
    var firstItem = true
    while (item != null) {
        if (item is ListItem) {
            val marker = if (ordered) "$index. " else "• "
            val content =
                buildAnnotatedString {
                    appendMarkdownBlocks(item, fontSize, textColor, linkColor, compact = true)
                }.trimTrailingNewlines()
            if (!firstItem) append("\n")
            if (content.text.isEmpty()) {
                append(marker.trimEnd())
            } else {
                appendPrefixedLines(content, marker)
            }
            firstItem = false
            index++
        }
        item = item.next
    }
    append("\n\n")
}

/**
 * Appends [content] line by line, with [firstPrefix] on the first line and
 * [continuationPrefix] on the rest. This is what keeps a nested list under its
 * parent item and every line of a blockquote behind its quote marker.
 */
private fun AnnotatedString.Builder.appendPrefixedLines(
    content: AnnotatedString,
    firstPrefix: String,
    continuationPrefix: String = " ".repeat(firstPrefix.length),
) {
    val text = content.text
    if (text.isEmpty()) return
    var start = 0
    var firstLine = true
    while (start <= text.length) {
        val newline = text.indexOf('\n', start)
        val end = if (newline < 0) text.length else newline
        if (!firstLine) append("\n")
        append(if (firstLine) firstPrefix else continuationPrefix)
        append(content.subSequence(start, end))
        firstLine = false
        if (newline < 0) break
        start = newline + 1
    }
}

private fun AnnotatedString.Builder.appendCodeBlock(literal: String?) {
    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
        append(literal.orEmpty().trimEnd('\n'))
    }
    append("\n\n")
}

private fun headingFactor(level: Int): Float =
    when (level) {
        1 -> 1.5f
        2 -> 1.3f
        3 -> 1.18f
        else -> 1.08f
    }

private fun AnnotatedString.trimTrailingNewlines(): AnnotatedString {
    val end = text.indexOfLast { it != '\n' } + 1
    return if (end <= 0) this else subSequence(0, end)
}
