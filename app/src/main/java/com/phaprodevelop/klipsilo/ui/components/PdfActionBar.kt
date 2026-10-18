package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.withoutSoftHyphens

internal data class PdfBarActionSpec(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * PDF viewer bottom bar (#45). Labels share one size and at most two lines
 * when the row fits. Otherwise each action becomes an icon with a tooltip,
 * and whatever still does not fit moves into the overflow menu. The menu
 * button is always placed on screen; the bar does not scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfViewerActionBar(
    pageIndex: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToPage: (Int) -> Unit,
    actions: List<PdfBarActionSpec>,
    menu: List<PdfBarActionSpec>,
    moreLabel: String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    var menuOpen by remember { mutableStateOf(false) }
    SubcomposeLayout(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
    ) { constraints ->
        val maxWidth = constraints.maxWidth
        val overflowPx = 40.dp.roundToPx()
        val iconOnlyPx = 40.dp.roundToPx()
        val labelPadPx = 12.dp.roundToPx()
        val labelSlopPx = 4.dp.roundToPx()
        val iconGraphicPx = 22.dp.roundToPx()
        val pagerMax = (maxWidth - overflowPx).coerceAtLeast(0)
        val pager =
            if (pageCount > 1) {
                subcompose("pager") {
                    PageSelector(
                        pageIndex = pageIndex,
                        pageCount = pageCount,
                        onPrev = onPrev,
                        onNext = onNext,
                        onGoToPage = onGoToPage,
                    )
                }.first().measure(Constraints(maxWidth = pagerMax))
            } else {
                null
            }
        val pinned = pager?.width ?: 0
        val base = labelStyle.fontSize.value

        fun widthOf(
            text: String,
            scale: Float,
        ): Float = textWidthPx(measurer, text, labelStyle, base, scale)
        val labelledAtScale =
            if (base <= 0f) {
                emptyList()
            } else {
                fitScales(LABEL_FONT_FLOOR_FRACTION).mapNotNull { scale ->
                    val widths =
                        actions.map { action ->
                            val wanted =
                                minWidthForLines(action.label, scale, maxLines = 2, ::widthOf)
                                    .coerceAtLeast(1f)
                            val decision = resolveAtScale(action.label, wanted, scale, maxLines = 2, ::widthOf)
                            if (decision.ellipsized) {
                                null
                            } else {
                                // Padding in the button can exceed the text measure by a few pixels.
                                maxOf(iconGraphicPx.toFloat(), wanted) + labelPadPx + labelSlopPx
                            }
                        }
                    if (widths.all { it != null }) scale to widths.filterNotNull() else null
                }
            }
        val plan =
            planActionBar(
                availablePx = maxWidth.toFloat(),
                overflowPx = overflowPx.toFloat(),
                pinnedPx = pinned.toFloat(),
                iconOnlyPx = List(actions.size) { iconOnlyPx.toFloat() },
                labelledPxAtScale = labelledAtScale,
            )
        val limit = (maxWidth - overflowPx).coerceAtLeast(0)

        fun measureMode(
            mode: BarItemMode,
            index: Int,
            slot: String,
        ): androidx.compose.ui.layout.Placeable? {
            val action = actions[index]
            return when (mode) {
                BarItemMode.Labelled -> {
                    val scale = plan.labelScale ?: 1f
                    val wanted =
                        minWidthForLines(action.label, scale, maxLines = 2, ::widthOf).coerceAtLeast(1f)
                    val decision = resolveAtScale(action.label, wanted, scale, maxLines = 2, ::widthOf)
                    subcompose(slot) {
                        LabelledPdfAction(action, decision, labelStyle)
                    }.first().measure(Constraints())
                }
                BarItemMode.IconOnly ->
                    subcompose(slot) { IconPdfAction(action) }.first().measure(Constraints())
                BarItemMode.Overflow -> null
            }
        }

        val modes = plan.modes.toMutableList()

        fun measureAll(): List<androidx.compose.ui.layout.Placeable?> =
            modes.mapIndexed { index, mode ->
                if (mode == BarItemMode.Overflow) {
                    // Replace an earlier labelled measure of this slot so it is not left composed.
                    subcompose("item-$index") {}
                    null
                } else {
                    measureMode(mode, index, "item-$index")
                }
            }

        fun usedWidth(items: List<androidx.compose.ui.layout.Placeable?>): Int {
            return pinned + items.sumOf { it?.width ?: 0 }
        }

        var measured = measureAll()
        // Text estimates can be a few pixels under the real button. If the
        // row would cover the menu button, drop actions to icons, then to
        // the menu (#45).
        if (usedWidth(measured) > limit && modes.any { it == BarItemMode.Labelled }) {
            var used = pinned
            for (index in modes.indices) {
                if (used + iconOnlyPx <= limit) {
                    modes[index] = BarItemMode.IconOnly
                    used += iconOnlyPx
                } else {
                    modes[index] = BarItemMode.Overflow
                }
            }
            measured = measureAll()
        }
        if (usedWidth(measured) > limit) {
            var used = pinned
            for (index in modes.indices) {
                val width = measured[index]?.width ?: 0
                if (modes[index] != BarItemMode.Overflow && used + width <= limit) {
                    used += width
                } else {
                    modes[index] = BarItemMode.Overflow
                }
            }
            measured = measureAll()
        }
        val overflowed =
            actions.filterIndexed { index, _ -> modes[index] == BarItemMode.Overflow } + menu
        val overflow =
            subcompose("overflow") {
                OverflowMenuButton(
                    moreLabel = moreLabel,
                    expanded = menuOpen,
                    onExpanded = { menuOpen = it },
                    entries = overflowed,
                )
            }.first().measure(Constraints())
        val visible = measured.filterNotNull()
        val height =
            listOfNotNull(pager?.height, overflow.height)
                .plus(visible.map { it.height })
                .maxOrNull()
                ?.coerceAtLeast(0) ?: 0
        layout(maxWidth, height) {
            var x = 0
            pager?.placeRelative(0, (height - pager.height) / 2)
            x += pinned
            for (placeable in visible) {
                if (x + placeable.width > limit) break
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width
            }
            overflow.placeRelative((maxWidth - overflow.width).coerceAtLeast(0), (height - overflow.height) / 2)
        }
    }
}

@Composable
private fun LabelledPdfAction(
    action: PdfBarActionSpec,
    decision: FitTextDecision,
    style: TextStyle,
) {
    val base = style.fontSize.value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = action.onClick)
                .clearAndSetSemantics { contentDescription = action.label.withoutSoftHyphens() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = decision.text,
            style = scaledLabelStyle(style, base * decision.scale, base),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = decision.maxLines,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPdfAction(action: PdfBarActionSpec) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(action.label) } },
        state = rememberTooltipState(),
    ) {
        CompactIconButton(onClick = action.onClick) {
            Icon(
                action.icon,
                contentDescription = action.label.withoutSoftHyphens(),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverflowMenuButton(
    moreLabel: String,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    entries: List<PdfBarActionSpec>,
) {
    Box {
        CompactIconButton(onClick = { onExpanded(true) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = moreLabel.withoutSoftHyphens())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null) },
                    onClick = {
                        onExpanded(false)
                        entry.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            content()
        }
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Preview(fontScale = 2f, widthDp = 360, showBackground = true)
@Composable
private fun PdfViewerActionBarLargePreview() {
    MaterialTheme {
        PdfViewerActionBar(
            pageIndex = 0,
            pageCount = 3,
            onPrev = {},
            onNext = {},
            onGoToPage = {},
            actions =
                listOf(
                    PdfBarActionSpec(Icons.Filled.PictureAsPdf, "Seite speichern") {},
                    PdfBarActionSpec(Icons.Filled.TextFields, "OCR-Seite") {},
                ),
            menu = emptyList(),
            moreLabel = "Mehr",
        )
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
private fun PdfViewerActionBar320Preview() {
    MaterialTheme {
        PdfViewerActionBar(
            pageIndex = 0,
            pageCount = 3,
            onPrev = {},
            onNext = {},
            onGoToPage = {},
            actions =
                listOf(
                    PdfBarActionSpec(Icons.Filled.PictureAsPdf, "Seite speichern") {},
                    PdfBarActionSpec(Icons.Filled.TextFields, "OCR-Seite") {},
                ),
            menu = listOf(PdfBarActionSpec(Icons.Filled.MoreVert, "PDF → Image") {}),
            moreLabel = "More options",
        )
    }
}
