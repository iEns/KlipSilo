package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A widget rectangle in PDF user space. Origin is the bottom left of the page. */
internal data class PdfUserRect(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float,
)

/** One extracted text run in PDF user space. [baseline] is the text baseline. */
internal data class PdfTextRun(
    val text: String,
    val left: Float,
    val baseline: Float,
    val width: Float,
    val height: Float,
)

/** Widget rectangle as fractions of the upright (visually rotated) page. */
internal data class VisualFieldRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal sealed interface ResolvedFieldLabel {
    data class Literal(
        val text: String,
    ) : ResolvedFieldLabel

    data class Numbered(
        val kind: FormFieldType,
        val number: Int,
    ) : ResolvedFieldLabel
}

private val genericFieldName =
    Regex(
        """(?ix)^(
            (?:s|f|cb|chk|check|checkbox|field|text|txt|tx|radio|btn|button|untitled|unnamed|af|tf|box)
            [\s._-]*\d+
            |(?:check\s*box|text\s*field|textfield|radio\s*button|radiobutton)[\s._-]*\d*
            |\d+
        )$""",
    )

/**
 * Tooltip (TU) wins. Otherwise a readable partial name is title-cased.
 * Cryptic names such as "s0" use nearby page text, then "Checkbox 1".
 */
internal fun resolveFormFieldLabel(
    partialName: String,
    alternateName: String?,
    type: FormFieldType,
    nearbyText: String?,
    fallbackNumber: Int,
): ResolvedFieldLabel {
    val tooltip = alternateName?.trim().orEmpty()
    if (tooltip.isNotEmpty()) return ResolvedFieldLabel.Literal(tooltip)
    val partial = partialName.trim()
    if (partial.isNotEmpty() && !genericFieldName.matches(partial)) {
        return ResolvedFieldLabel.Literal(prettifyFieldName(partial))
    }
    val nearby = nearbyText?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    if (nearby.isNotEmpty()) return ResolvedFieldLabel.Literal(nearby.take(80))
    return ResolvedFieldLabel.Numbered(type, fallbackNumber)
}

/** Keyboard the on-screen keyboard should use for one form field. */
enum class FormKeyboardKind {
    TEXT,
    EMAIL,
    PHONE,
    NUMBER,
}

/** Bits of a text field's /Ff entry that change how the field looks on screen. */
data class TextFieldFlagBits(
    val multiline: Boolean,
    val password: Boolean,
    val comb: Boolean,
)

/** Bit 13 of /Ff (zero based 12): the text field spans several lines. */
const val TEXT_FIELD_FLAG_MULTILINE = 1L shl 12

/** Bit 14 of /Ff (zero based 13): the text field is a password field. */
const val TEXT_FIELD_FLAG_PASSWORD = 1L shl 13

/** Bit 29 of /Ff (zero based 28): the text field is divided into equal cells. */
const val TEXT_FIELD_FLAG_COMB = 1L shl 28

internal fun parseTextFieldFlags(fieldFlags: Long?): TextFieldFlagBits {
    val flags = fieldFlags ?: 0L
    return TextFieldFlagBits(
        multiline = flags and TEXT_FIELD_FLAG_MULTILINE != 0L,
        password = flags and TEXT_FIELD_FLAG_PASSWORD != 0L,
        comb = flags and TEXT_FIELD_FLAG_COMB != 0L,
    )
}

private val emailKeyboardWords = setOf("email", "mail")

private val phoneKeyboardWords = setOf("phone", "tel", "telefon", "mobil")

private val numberKeyboardWords = setOf("number", "zip", "postnr", "postcode", "age", "alder", "year")

/**
 * Keyboard type from the field's name, tooltip and label. URL/email-like words suggest an email
 * keyboard, phone words a phone keyboard, count words a number keyboard; anything else stays text
 * (including date fields, where the typed separators vary too much for a numeric keyboard).
 */
internal fun formKeyboardKind(
    name: String?,
    alternateName: String?,
    label: String?,
): FormKeyboardKind {
    val haystack = listOfNotNull(name, alternateName, label).joinToString(" ").lowercase(Locale.ROOT)
    if (haystack.contains("email")) return FormKeyboardKind.EMAIL
    val words = haystack.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    if (words.any { it in emailKeyboardWords }) return FormKeyboardKind.EMAIL
    if (words.any { it in phoneKeyboardWords }) return FormKeyboardKind.PHONE
    if (words.any { it in numberKeyboardWords }) return FormKeyboardKind.NUMBER
    return FormKeyboardKind.TEXT
}

internal fun prettifyFieldName(raw: String): String {
    val spaced =
        raw
            .replace(Regex("[_./]+"), " ")
            .replace(Regex("(?<=[a-z0-9])([A-Z])")) { match ->
                " ${match.groupValues[1].lowercase(Locale.ROOT)}"
            }
            .replace(Regex("\\s+"), " ")
            .trim()
    if (spaced.isEmpty()) return raw.trim()
    return spaced.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
    }
}

/**
 * Label printed beside or above a widget. Prefers text on the left, then text just above.
 */
internal fun nearbyFieldText(
    widget: PdfUserRect,
    runs: List<PdfTextRun>,
): String? {
    val lines = groupRuns(runs)
    val widgetHeight = (widget.top - widget.bottom).coerceAtLeast(1f)
    val left =
        lines
            .filter { line ->
                line.text.length <= 80 &&
                    line.right <= widget.left + 8f &&
                    widget.left - line.right < 140f &&
                    verticalOverlap(line, widget) >= widgetHeight * 0.3f
            }.minByOrNull { widget.left - it.right }
    if (left != null) return left.text
    return lines
        .filter { line ->
            line.text.length <= 80 &&
                line.bottom >= widget.top - 4f &&
                line.bottom - widget.top < 36f &&
                horizontalOverlap(line.left, line.right, widget.left, widget.right) > 0f
        }.minByOrNull { it.bottom - widget.top }
        ?.text
}

/**
 * Label printed to the right of a widget. Checkbox and radio boxes in a group draw their option
 * text on the right, which is what distinguishes them from each other.
 */
internal fun nearbyFieldTextRight(
    widget: PdfUserRect,
    runs: List<PdfTextRun>,
): String? {
    val lines = groupRuns(runs)
    val widgetHeight = (widget.top - widget.bottom).coerceAtLeast(1f)
    return lines
        .filter { line ->
            line.text.length <= 80 &&
                line.left >= widget.right - 8f &&
                line.left - widget.right < 140f &&
                verticalOverlap(line, widget) >= widgetHeight * 0.3f
        }.minByOrNull { it.left - widget.right }
        ?.text
}

private val ambiguousCheckOptions =
    setOf("x", "0", "1", "no", "off", "on", "true", "false", "yes", "ja", "nej", "checked", "selected")

/** A checkbox export value like "Yes" or "1" names no option by itself. */
internal fun isAmbiguousCheckOption(value: String): Boolean {
    return value.trim().lowercase(Locale.ROOT) in ambiguousCheckOptions
}

/**
 * Label for one checkbox child inside a group. All children share the group name, so the
 * widget's own tooltip wins, then its on-state export value ("Morning_session" -> "Morning
 * session"), then the page text to the right of the widget. An ambiguous export value ("Yes")
 * falls back to that nearby text and finally to "Group: Yes"; nothing at all falls back into
 * the ordinary label chain with the group name as the field name.
 */
internal fun resolveCheckboxKidLabel(
    tooltip: String?,
    onState: String?,
    nearbyRight: String?,
    nearbyElsewhere: String?,
    group: String?,
    type: FormFieldType,
    fallbackNumber: Int,
): ResolvedFieldLabel {
    val tip = tooltip?.trim().orEmpty()
    if (tip.isNotEmpty()) return ResolvedFieldLabel.Literal(tip)
    val state = onState?.trim().orEmpty()
    val readableState = state.isNotEmpty() && !isAmbiguousCheckOption(state)
    if (readableState) return ResolvedFieldLabel.Literal(prettifyFieldName(state))
    val right = nearbyRight?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    if (right.isNotEmpty()) return ResolvedFieldLabel.Literal(right.take(80))
    if (state.isNotEmpty() && !group.isNullOrBlank()) {
        return ResolvedFieldLabel.Literal("${prettifyFieldName(group)}: ${prettifyFieldName(state)}")
    }
    return resolveFormFieldLabel(
        partialName = group.orEmpty(),
        alternateName = null,
        type = type,
        nearbyText = nearbyElsewhere,
        fallbackNumber = fallbackNumber,
    )
}

/**
 * Maps a widget to fractions of the page bitmap. [rotation] is the page's clockwise /Rotate.
 */
internal fun widgetVisualFractions(
    widget: PdfUserRect,
    pageLeft: Float,
    pageBottom: Float,
    pageWidth: Float,
    pageHeight: Float,
    rotation: Int,
): VisualFieldRect? {
    if (pageWidth <= 0f || pageHeight <= 0f) return null
    val rot = ((rotation % 360) + 360) % 360
    val visualWidth = if (rot == 90 || rot == 270) pageHeight else pageWidth
    val visualHeight = if (rot == 90 || rot == 270) pageWidth else pageHeight
    if (visualWidth <= 0f || visualHeight <= 0f) return null
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    val xs = floatArrayOf(widget.left, widget.right)
    val ys = floatArrayOf(widget.bottom, widget.top)
    for (x in xs) {
        for (y in ys) {
            val relativeX = x - pageLeft
            val relativeY = y - pageBottom
            val (visualX, visualY) =
                when (rot) {
                    90 -> relativeY to relativeX
                    180 -> (pageWidth - relativeX) to relativeY
                    270 -> (pageHeight - relativeY) to (pageWidth - relativeX)
                    else -> relativeX to (pageHeight - relativeY)
                }
            minX = min(minX, visualX)
            maxX = max(maxX, visualX)
            minY = min(minY, visualY)
            maxY = max(maxY, visualY)
        }
    }
    return VisualFieldRect(
        left = (minX / visualWidth).coerceIn(0f, 1f),
        top = (minY / visualHeight).coerceIn(0f, 1f),
        right = (maxX / visualWidth).coerceIn(0f, 1f),
        bottom = (maxY / visualHeight).coerceIn(0f, 1f),
    )
}

private data class TextLine(
    val text: String,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val baseline: Float,
    val height: Float,
)

private fun groupRuns(runs: List<PdfTextRun>): List<TextLine> {
    val lines = mutableListOf<TextLine>()
    val sorted = runs.sortedWith(compareByDescending<PdfTextRun> { it.baseline }.thenBy { it.left })
    for (run in sorted) {
        val text = run.text.replace(Regex("\\s+"), " ").trim()
        if (text.length < 2 || run.width <= 0f) continue
        val height = run.height.coerceAtLeast(1f)
        val top = run.baseline + height * 0.25f
        val bottom = run.baseline - height * 0.85f
        val right = run.left + run.width
        val previous = lines.lastOrNull()
        val sameLine =
            previous != null &&
                abs(previous.baseline - run.baseline) <= max(2f, height * 0.45f) &&
                run.left - previous.right < height * 1.5f
        if (previous != null && sameLine) {
            lines[lines.lastIndex] =
                previous.copy(
                    text = "${previous.text} $text".trim(),
                    right = max(previous.right, right),
                    top = max(previous.top, top),
                    bottom = min(previous.bottom, bottom),
                )
        } else {
            lines.add(TextLine(text, run.left, right, top, bottom, run.baseline, height))
        }
    }
    return lines
}

private fun verticalOverlap(
    line: TextLine,
    widget: PdfUserRect,
): Float {
    val top = min(line.top, widget.top)
    val bottom = max(line.bottom, widget.bottom)
    return (top - bottom).coerceAtLeast(0f)
}

private fun horizontalOverlap(
    left: Float,
    right: Float,
    otherLeft: Float,
    otherRight: Float,
): Float {
    return (min(right, otherRight) - max(left, otherLeft)).coerceAtLeast(0f)
}
