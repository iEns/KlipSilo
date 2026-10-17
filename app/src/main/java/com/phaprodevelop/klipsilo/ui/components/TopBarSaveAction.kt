package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextMeasurer
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
 * Bars this narrow always collapse the text action to its icon (#47): on a
 * 320dp screen even a short label like "Speichern" leaves a title cramped
 * against the pill, and the batch-2 retest showed the three top bars
 * disagreeing there (Mark up kept its label while Fill form and Edit pages
 * collapsed). Jens decided the ✓ is correct at 320dp, so the same rule must
 * yield it on every screen.
 */
internal const val TOP_BAR_COMPACT_MAX_WIDTH_DP = 360

/**
 * True when a top bar text action must collapse to an icon-only button
 * (#47). Every top bar uses the same inputs, so the three screens decide
 * alike: the font scale, the bar width, the width the title needs at its
 * floor size ([titleWidthPx], widest word when wrapped to two lines) and the
 * measured action label width ([actionLabelWidthPx], padding added by
 * caller). [leadingWidthPx] is the navigation icon area.
 */
internal fun topBarActionCollapses(
    fontScale: Float,
    barWidthPx: Float,
    pxPerDp: Float,
    leadingWidthPx: Float,
    titleWidthPx: Float,
    actionLabelWidthPx: Float,
): Boolean {
    if (fontScale >= TOP_BAR_ICON_FONT_SCALE) return true
    if (barWidthPx <= 0f || pxPerDp <= 0f) return false
    if (barWidthPx / pxPerDp <= TOP_BAR_COMPACT_MAX_WIDTH_DP) return true
    val titleSpace = barWidthPx - leadingWidthPx - actionLabelWidthPx
    return titleSpace < barWidthPx * TOP_BAR_MIN_TITLE_FRACTION || titleSpace < titleWidthPx
}

/**
 * Save-style top bar action shared by every top bar with a text action (#47).
 * At normal scale on a wide bar this is the usual labelled button; at large
 * font scale, on a compact bar (≤ [TOP_BAR_COMPACT_MAX_WIDTH_DP]) or whenever
 * the label would squeeze the title unreadable, it becomes an icon-only ✓
 * button with the localized [label] as content description and a
 * [androidx.compose.material3.PlainTooltip], so the title keeps its space and
 * stays readable. [title] is the title shown next to the action; the collapse
 * decision accounts for its width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSaveAction(
    label: String,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rememberTopBarActionCollapses(label, title)) {
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
 * Top bar title (#47). Two lines at the style size, then a shrink down to
 * [TOP_BAR_TITLE_MIN_SCALE], then an ellipsis. The bar that hosts it must
 * grow with the text: a fixed 64dp row clips the second line.
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
        maxLines = 2,
        floorFraction = TOP_BAR_TITLE_MIN_SCALE,
    )
}

/**
 * Top bar whose height follows the title (#47). Material's single-row
 * [androidx.compose.material3.TopAppBar] is 64dp, which at font scale 2.0
 * leaves a German title ellipsized on one line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTopBar(
    title: String,
    saveLabel: String,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                )
            }
            TopBarFitTitle(text = title, modifier = Modifier.weight(1f))
            TopBarSaveAction(
                label = saveLabel,
                title = title,
                enabled = saveEnabled,
                onClick = onSave,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

/** The measured title width at its floor size, widest word when wrapped. */
internal fun titleFloorWidthPx(
    measurer: TextMeasurer,
    title: String,
    style: TextStyle,
): Float {
    if (style.fontSize.value <= 0f) return 0f
    val floor = style.fontSize.value * TOP_BAR_TITLE_MIN_SCALE
    return title
        .split(Regex("""\s+"""))
        .filter { it.isNotEmpty() }
        .maxOfOrNull { word ->
            measurer
                .measure(
                    text = word,
                    style = scaledLabelStyle(style, floor, style.fontSize.value),
                    softWrap = false,
                    maxLines = 1,
                ).size.width
                .toFloat()
        }
        ?: 0f
}

@Composable
private fun rememberTopBarActionCollapses(
    label: String,
    title: String,
): Boolean {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleLarge
    // The top bar spans the window (the screens use zero window insets), so
    // the configuration width is the bar width (#47).
    val barWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val leadingWidthPx = with(density) { 56.dp.toPx() }
    val titleWidthPx =
        remember(title, titleStyle, density.fontScale) {
            titleFloorWidthPx(measurer, title, titleStyle)
        }
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
        pxPerDp = density.density,
        leadingWidthPx = leadingWidthPx,
        titleWidthPx = titleWidthPx,
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
@Preview(fontScale = 2f, widthDp = 360, showBackground = true)
@Composable
private fun TopBarFitTitlePreview() {
    MaterialTheme {
        FitTopBar(
            title = "Formular ausfüllen",
            saveLabel = "PDF speichern",
            saveEnabled = true,
            onBack = {},
            onSave = {},
            backContentDescription = "Zurück",
        )
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun TopBarFitTitleDanishPreview() {
    MaterialTheme {
        FitTopBar(
            title = "Udfyld formular",
            saveLabel = "Gem PDF",
            saveEnabled = true,
            onBack = {},
            onSave = {},
            backContentDescription = "Tilbage",
        )
    }
}

@Preview(fontScale = 2f, widthDp = 360, showBackground = true)
@Composable
private fun TopBarEditPagesPreview() {
    MaterialTheme {
        FitTopBar(
            title = "Seiten bearbeiten",
            saveLabel = "PDF speichern",
            saveEnabled = true,
            onBack = {},
            onSave = {},
            backContentDescription = "Zurück",
        )
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
        TopBarSaveAction(
            label = label,
            title = title,
            enabled = true,
            onClick = {},
        )
    }
}
