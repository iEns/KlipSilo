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
        appendMarkdownNode(root, fontSize, textColor, linkColor)
    }.trimTrailingNewlines()
}

private fun AnnotatedString.Builder.appendMarkdownNode(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
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
                appendChildren(node, fontSize, textColor, linkColor)
            }
            append("\n\n")
        }
        is Paragraph -> {
            appendChildren(node, fontSize, textColor, linkColor)
            append("\n\n")
        }
        is StrongEmphasis ->
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendChildren(node, fontSize, textColor, linkColor)
            }
        is Emphasis ->
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendChildren(node, fontSize, textColor, linkColor)
            }
        is Strikethrough ->
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendChildren(node, fontSize, textColor, linkColor)
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
        is FencedCodeBlock -> appendCodeBlock(node.literal)
        is IndentedCodeBlock -> appendCodeBlock(node.literal)
        is BlockQuote -> {
            withStyle(SpanStyle(color = textColor.copy(alpha = 0.8f))) {
                append("› ")
                appendChildren(node, fontSize, textColor, linkColor)
            }
            append("\n")
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
        is ListItem -> appendChildren(node, fontSize, textColor, linkColor)
        is Link ->
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                appendChildren(node, fontSize, textColor, linkColor)
            }
        is Image -> append(node.title ?: node.destination.orEmpty())
        is Text -> append(node.literal)
        is SoftLineBreak -> append(" ")
        is HardLineBreak -> append("\n")
        is ThematicBreak -> append("────────\n\n")
        else -> appendChildren(node, fontSize, textColor, linkColor)
    }
}

private fun AnnotatedString.Builder.appendChildren(
    node: Node,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
) {
    var child = node.firstChild
    while (child != null) {
        appendMarkdownNode(child, fontSize, textColor, linkColor)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendList(
    list: Node,
    ordered: Boolean,
    fontSize: Float,
    textColor: Color,
    linkColor: Color,
) {
    var index = 1
    var item = list.firstChild
    while (item != null) {
        append(if (ordered) "$index. " else "• ")
        appendChildren(item, fontSize, textColor, linkColor)
        if (item.next != null) append("\n")
        index++
        item = item.next
    }
    append("\n\n")
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
