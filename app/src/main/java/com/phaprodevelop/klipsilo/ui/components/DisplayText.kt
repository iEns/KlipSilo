package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Plain display text that may carry invisible break characters (#64): the
 * soft hyphen U+00AD that Grok Build inserts into long localized DE/DA
 * strings, and the zero-width space U+200B that display rules insert
 * ([com.phaprodevelop.klipsilo.data.withDisplayBreaks],
 * `insertDisplayBreaksAfterPlusAndMinus`). The display form is rendered
 * UNCHANGED — the marks still steer the line breaking and are drawn as a
 * hyphen where the line breaks — while the semantics read the clean source
 * string via the existing [fitTextSemantics] rule, exactly as [ButtonLabel]
 * does for its labels. A screen reader never sees either mark.
 *
 * Use it for every `Text` that shows a display-broken string or a localized
 * string whose translations may contain U+00AD (Tips, Limitations, Use
 * cases, Why, the Info rows, sheet rows, ...). Labels inside buttons, chips
 * and row titles keep [ButtonLabel] — same semantics rule, plus the fit-text
 * layout.
 *
 * No [androidx.compose.ui.text.AnnotatedString] overload: nothing in the app
 * renders a localized string as a linked AnnotatedString — the markdown
 * `md_*` entries render through [ButtonLabel] with plain concatenated
 * strings, so `clearAndSetSemantics` never has link/clickable child
 * semantics to preserve. If a linked variant is ever needed, override only
 * the `text` property (`semantics { text = ... }`) instead of clearing, so
 * the link annotations survive.
 */
@Composable
fun DisplayText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
) {
    Text(
        text = text,
        // The semantics read the source string, not the display form: the
        // drawn form carries the invisible break characters (#64).
        modifier = modifier.fitTextSemantics(text),
        color = color,
        fontWeight = fontWeight,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        softWrap = softWrap,
    )
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun DisplayTextLargeScalePreview() {
    // #64: the soft hyphen still hyphenates the display ("Dank-/sagungen")
    // and the zero-width space still breaks after "_", while TalkBack reads
    // "Open-Source-Danksagungen" / "Event_Registration_Form".
    MaterialTheme {
        Column(Modifier.padding(8.dp)) {
            DisplayText(
                text = "Open-Source-Dank\u00ADsagungen",
                style = MaterialTheme.typography.titleMedium,
            )
            DisplayText(
                text = "Event_\u200BRegistration_\u200BForm",
                style = MaterialTheme.typography.bodyMedium,
            )
            DisplayText(
                text = "Zeilen\u00ADumbrüche und Silben\u00ADtrennung weichen ab",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
