package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Font scale from which a top bar text action collapses to an icon-only
 * button (#47): at larger scales the labelled pill squeezes the title into
 * an unreadable "Anm…" / "A…".
 */
internal const val TOP_BAR_ICON_FONT_SCALE = 1.5f

/**
 * Fraction of the top bar width that must stay available for the title
 * (#47). When the labelled action would leave less than this, it collapses
 * to an icon-only button even at normal font scale.
 */
internal const val TOP_BAR_MIN_TITLE_FRACTION = 0.4f

/**
 * True when a top bar text action must collapse to an icon-only button
 * (#47): either the font scale is too large for a labelled pill to leave a
 * readable title, or the label would leave less than
 * [TOP_BAR_MIN_TITLE_FRACTION] of the [barWidthPx] for the title.
 * [leadingWidthPx] is the navigation icon area; [actionLabelWidthPx] the
 * measured single-line width of the action label (padding added by caller).
 */
internal fun topBarActionCollapses(
    fontScale: Float,
    barWidthPx: Float,
    leadingWidthPx: Float,
    actionLabelWidthPx: Float,
): Boolean {
    if (fontScale >= TOP_BAR_ICON_FONT_SCALE) return true
    if (barWidthPx <= 0f) return false
    val titleSpace = barWidthPx - leadingWidthPx - actionLabelWidthPx
    return titleSpace < barWidthPx * TOP_BAR_MIN_TITLE_FRACTION
}

/**
 * Save-style top bar action shared by every top bar with a text action (#47).
 * At normal scale this is the usual labelled button; at large font scale (or
 * whenever the label would squeeze the title below [TOP_BAR_MIN_TITLE_FRACTION]
 * of the bar) it becomes an icon-only ✓ button with the localized [label] as
 * content description and a [androidx.compose.material3.PlainTooltip], so the
 * title keeps its space and stays readable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSaveAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rememberTopBarActionCollapses(label)) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
                Icon(Icons.Filled.Check, contentDescription = label)
            }
        }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) {
            ButtonLabel(label)
        }
    }
}

/**
 * Top bar title that stays on one line and shrinks (floor ~75% of the style
 * size, reusing the [ButtonLabel] fitting logic) before falling back to an
 * ellipsis (#47).
 */
@Composable
fun TopBarFitTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    ButtonLabel(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
    )
}

@Composable
private fun rememberTopBarActionCollapses(label: String): Boolean {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val measurer = rememberTextMeasurer()
    val barWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val leadingWidthPx = with(density) { 56.dp.toPx() }
    val labelWidthPx =
        remember(label, density.fontScale) {
            measurer
                .measure(
                    text = label,
                    style = TextStyle(fontSize = 14.sp),
                    softWrap = false,
                    maxLines = 1,
                ).size.width
                .toFloat()
        }
    // Button content padding (24dp each side) plus the end inset of the
    // actions area.
    val actionWidthPx = labelWidthPx + with(density) { 64.dp.toPx() }
    return topBarActionCollapses(
        fontScale = density.fontScale,
        barWidthPx = barWidthPx,
        leadingWidthPx = leadingWidthPx,
        actionLabelWidthPx = actionWidthPx,
    )
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun TopBarSaveActionLargeScalePreview() {
    MaterialTheme {
        TopBarRow(title = "Anmerken", label = "Speichern")
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
private fun TopBarSaveActionNormalScalePreview() {
    MaterialTheme {
        TopBarRow(title = "Anmerken", label = "Speichern")
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun TopBarFitTitlePreview() {
    MaterialTheme {
        TopBarRow(title = "Udfyld formular", label = "Gem PDF")
    }
}

@Composable
private fun TopBarRow(
    title: String,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarFitTitle(title, modifier = Modifier.weight(1f))
        TopBarSaveAction(label = label, enabled = true, onClick = {})
    }
}
