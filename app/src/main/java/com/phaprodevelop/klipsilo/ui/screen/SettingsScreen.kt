package com.phaprodevelop.klipsilo.ui.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.settings.PdfFont
import com.phaprodevelop.klipsilo.data.settings.PdfSettings
import com.phaprodevelop.klipsilo.data.settings.PdfSettingsStore
import com.phaprodevelop.klipsilo.data.settings.ThemeMode
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.FitTextPolicy
import com.phaprodevelop.klipsilo.ui.components.LABEL_FONT_FLOOR_FRACTION
import com.phaprodevelop.klipsilo.ui.components.LARGE_FONT_SCALE
import com.phaprodevelop.klipsilo.ui.components.fitTextSemantics
import com.phaprodevelop.klipsilo.ui.components.rememberUniformLabelDecisions
import com.phaprodevelop.klipsilo.ui.components.scaledLabelStyle
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import java.util.Locale

/** Settings labels shrink to this fraction of their size before an ellipsis (#62). */
private const val SETTINGS_LABEL_MIN_SCALE = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClipboardViewModel,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.selectedLanguageTag(context)) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var pdfSettings by remember { mutableStateOf(PdfSettingsStore.load(context)) }
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLanguage =
        LocaleHelper.pickerLanguages.firstOrNull { it.tag == selectedLanguage }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            // Every Settings label follows the fit-text policy (#62): wrap at
            // word boundaries, shrink uniformly, ellipsis last — the German
            // compounds used to break mid-word in these rows.
            ButtonLabel(
                text = context.getString(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                floorFraction = SETTINGS_LABEL_MIN_SCALE,
            )
            Spacer(Modifier.size(16.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageSheet = true }
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Language,
                    null,
                    Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ButtonLabel(
                        text = context.getString(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        floorFraction = SETTINGS_LABEL_MIN_SCALE,
                    )
                    ButtonLabel(
                        text =
                            currentLanguage?.nativeName
                                ?: selectedLanguage
                                ?: context.getString(R.string.language_system),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        floorFraction = SETTINGS_LABEL_MIN_SCALE,
                    )
                }
                Text(
                    text = LocaleHelper.languageFlag(currentLanguage?.tag),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(8.dp))
                Text("›", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.size(24.dp))

            ButtonLabel(
                text = context.getString(R.string.display_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                floorFraction = SETTINGS_LABEL_MIN_SCALE,
            )
            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to context.getString(R.string.system),
                    ThemeMode.LIGHT to context.getString(R.string.light),
                    ThemeMode.DARK to context.getString(R.string.dark),
                ).forEach { (mode, label) ->
                    val selected = themeMode == mode
                    Card(
                        modifier = Modifier.weight(1f).clickable { viewModel.setThemeMode(mode) },
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                    ) {
                        ButtonLabel(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            contentAlignment = Alignment.Center,
                        )
                    }
                }
            }

            // #119: PDF & export is a settings row, so it sits directly under
            // the display-mode controls instead of at the very bottom below
            // the About links.
            Spacer(Modifier.size(24.dp))
            PdfExportSection(
                pdfSettings = pdfSettings,
                onSettingsChange = { updated ->
                    pdfSettings = updated
                    PdfSettingsStore.save(context, updated)
                },
            )

            Spacer(Modifier.size(24.dp))
            ButtonLabel(
                text = context.getString(R.string.about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                floorFraction = SETTINGS_LABEL_MIN_SCALE,
            )
            Spacer(Modifier.size(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAbout),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    ButtonLabel(
                        text = context.getString(R.string.about),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        floorFraction = SETTINGS_LABEL_MIN_SCALE,
                        hyphenate = true,
                    )
                    Text("›", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.size(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToPrivacy),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    ButtonLabel(
                        text = context.getString(R.string.privacy_policy),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        floorFraction = SETTINGS_LABEL_MIN_SCALE,
                        hyphenate = true,
                    )
                    Text("›", color = MaterialTheme.colorScheme.primary)
                }
            }
            // #163: a quiet Play Store rating link lives under Privacy, still
            // in the About block. No in-app review prompt or nag.
            Spacer(Modifier.size(12.dp))
            Text(
                text = context.getString(R.string.settings_rate_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { openPlayStore(context) },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    ButtonLabel(
                        text = context.getString(R.string.settings_rate_play),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        floorFraction = SETTINGS_LABEL_MIN_SCALE,
                        hyphenate = true,
                    )
                    Text("›", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            ) {
                LanguagePickerSheet(
                    selectedTag = selectedLanguage,
                    onSelect = { tag ->
                        showLanguageSheet = false
                        if (tag == selectedLanguage) return@LanguagePickerSheet
                        selectedLanguage = tag
                        LocaleHelper.setLanguage(context, tag)
                        (context as? Activity)?.recreate()
                    },
                )
            }
        }
    }
}

/**
 * #163: opens the production KlipSilo listing. The Play Store app handles
 * `market://`; when it is missing (or the launch fails) the web listing opens
 * in a browser instead. The production package id is hardcoded so `.beta`
 * builds also rate the released app — never [android.content.Context.getPackageName].
 */
private fun openPlayStore(context: Context) {
    val packageId = "com.phaprodevelop.klipsilo"
    val marketIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId"))
    if (marketIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(marketIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // No Play Store activity after all: fall through to the web listing.
        }
    }
    val webIntent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageId"),
        )
    try {
        context.startActivity(webIntent)
    } catch (_: ActivityNotFoundException) {
        // No browser either; the tap is a no-op rather than a crash.
    }
}

/**
 * #96: the PDF font cards and sliders stay behind one collapsed row, so the
 * rest of Settings stays reachable without scrolling past every PDF knob.
 * #119: the row sits directly under the display-mode controls and shows the
 * current font, so it reads as a settings row rather than a bare label.
 * Expanding shows the same settings as before.
 */
@Composable
internal fun PdfExportSection(
    pdfSettings: PdfSettings,
    onSettingsChange: (PdfSettings) -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            // #119: a title plus the current font makes this read like the
            // other Settings rows (Language shows its current value too),
            // instead of a bare section label above the PDF knobs.
            Column(Modifier.weight(1f)) {
                ButtonLabel(
                    text = context.getString(R.string.settings_pdf_export),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    floorFraction = SETTINGS_LABEL_MIN_SCALE,
                )
                ButtonLabel(
                    text = pdfFontLabel(context, pdfSettings.pdfFont),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    floorFraction = SETTINGS_LABEL_MIN_SCALE,
                )
            }
            Text(
                text = if (expanded) "▾" else "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (expanded) {
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PictureAsPdf,
                null,
                Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            ButtonLabel(
                text = context.getString(R.string.pdf_setting_font_family),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                floorFraction = SETTINGS_LABEL_MIN_SCALE,
            )
        }
        FontFamilyCards(
            selected = pdfSettings.pdfFont,
            onSelect = { font -> onSettingsChange(pdfSettings.copy(pdfFont = font)) },
        )
        Spacer(Modifier.size(8.dp))
        PdfSettingSlider(
            label = context.getString(R.string.pdf_setting_margin_top_bottom),
            value = pdfSettings.marginTopBottomCm,
            valueText =
                String.format(Locale.US, "%.1f", pdfSettings.marginTopBottomCm) +
                    " " + context.getString(R.string.unit_cm),
            valueRange = 0.5f..4.0f,
            steps = 34,
            onValueChange = { onSettingsChange(pdfSettings.copy(marginTopBottomCm = it)) },
        )
        PdfSettingSlider(
            label = context.getString(R.string.pdf_setting_margin_left_right),
            value = pdfSettings.marginLeftRightCm,
            valueText =
                String.format(Locale.US, "%.1f", pdfSettings.marginLeftRightCm) +
                    " " + context.getString(R.string.unit_cm),
            valueRange = 0.5f..4.0f,
            steps = 34,
            onValueChange = { onSettingsChange(pdfSettings.copy(marginLeftRightCm = it)) },
        )
        PdfSettingSlider(
            label = context.getString(R.string.pdf_setting_font_size),
            value = pdfSettings.fontSizePt,
            valueText = String.format(Locale.US, "%.0f pt", pdfSettings.fontSizePt),
            valueRange = 8f..28f,
            steps = 19,
            onValueChange = { onSettingsChange(pdfSettings.copy(fontSizePt = it)) },
        )
        PdfSettingSlider(
            label = context.getString(R.string.pdf_setting_line_spacing),
            value = pdfSettings.lineSpacing,
            valueText = String.format(Locale.US, "%.1f", pdfSettings.lineSpacing),
            valueRange = 1.0f..2.5f,
            steps = 14,
            onValueChange = { onSettingsChange(pdfSettings.copy(lineSpacing = it)) },
        )
        PdfSettingSlider(
            label = context.getString(R.string.pdf_setting_max_output),
            value = pdfSettings.maxOutputPx.toFloat(),
            valueText = String.format(Locale.US, "%d px", pdfSettings.maxOutputPx),
            valueRange = 1000f..6000f,
            steps = 49,
            onValueChange = { onSettingsChange(pdfSettings.copy(maxOutputPx = it.toInt())) },
        )
    }
}

/**
 * The four PDF font cards (#62). At large font scales they reflow into a 2×2
 * grid, and the four cards are ONE uniform group: a shared label scale from
 * [rememberUniformLabelDecisions], the policy wrapping each label at the
 * space, never inside a word and never clipped. Each label is drawn in the
 * family it selects (#125).
 */
@Composable
private fun FontFamilyCards(
    selected: PdfFont,
    onSelect: (PdfFont) -> Unit,
) {
    val context = LocalContext.current
    val options = PdfFont.entries.map { it to pdfFontLabel(context, it) }
    if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val slotWidthPx =
                (constraints.maxWidth - with(LocalDensity.current) { 8.dp.toPx() }.toInt()) / 2
            val style = MaterialTheme.typography.labelSmall
            val decisions =
                rememberUniformLabelDecisions(
                    labels = options.map { it.second },
                    slotWidthPx = slotWidthPx,
                    style = style,
                    policy = FitTextPolicy(maxLines = 2, minScale = LABEL_FONT_FLOOR_FRACTION),
                )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowOptions.forEachIndexed { columnIndex, (font, label) ->
                            val decision = decisions[rowIndex * 2 + columnIndex]
                            Card(
                                modifier = Modifier.weight(1f).clickable { onSelect(font) },
                                colors = fontCardColors(selected == font),
                            ) {
                                Text(
                                    text = decision.text,
                                    style =
                                        scaledLabelStyle(
                                            style.copy(fontFamily = font.composeFontFamily()),
                                            style.fontSize.value * decision.scale,
                                            style.fontSize.value,
                                        ),
                                    color = fontCardLabelColor(selected == font),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                            // Semantics from the source label,
                                            // not the drawn lines (#64).
                                            .fitTextSemantics(label),
                                    textAlign = TextAlign.Center,
                                    // Lines come from the policy, already
                                    // broken on word boundaries.
                                    softWrap = false,
                                    maxLines = decision.maxLines,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (font, label) ->
                val selectedFont = selected == font
                Card(
                    modifier = Modifier.weight(1f).clickable { onSelect(font) },
                    colors = fontCardColors(selectedFont),
                ) {
                    ButtonLabel(
                        text = label,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontFamily = font.composeFontFamily(),
                            ),
                        color = fontCardLabelColor(selectedFont),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        contentAlignment = Alignment.Center,
                    )
                }
            }
        }
    }
}

/** Localized name of [font], used by the PDF & export summary and font cards. */
private fun pdfFontLabel(
    context: Context,
    font: PdfFont,
): String =
    when (font) {
        PdfFont.SYSTEM -> context.getString(R.string.pdf_font_system)
        PdfFont.SANS -> context.getString(R.string.pdf_font_sans)
        PdfFont.SERIF -> context.getString(R.string.pdf_font_serif)
        PdfFont.MONO -> context.getString(R.string.pdf_font_mono)
    }

/**
 * #125: the font card label is drawn in the family it selects, so the choice
 * is visible before converting a clip.
 */
private fun PdfFont.composeFontFamily(): FontFamily =
    when (this) {
        PdfFont.SYSTEM -> FontFamily.Default
        PdfFont.SANS -> FontFamily.SansSerif
        PdfFont.SERIF -> FontFamily.Serif
        PdfFont.MONO -> FontFamily.Monospace
    }

@Composable
private fun fontCardColors(selected: Boolean) =
    CardDefaults.cardColors(
        containerColor =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
    )

@Composable
private fun fontCardLabelColor(selected: Boolean) =
    if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun LanguagePickerSheet(
    selectedTag: String?,
    onSelect: (String?) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
    ) {
        ButtonLabel(
            text = context.getString(R.string.language),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 2,
            floorFraction = SETTINGS_LABEL_MIN_SCALE,
        )
        LanguageOptionRow(
            flag = LocaleHelper.languageFlag(null),
            label = context.getString(R.string.language_system),
            selected = selectedTag == null,
            onClick = { onSelect(null) },
        )
        LocaleHelper.pickerLanguages.forEach { language ->
            LanguageOptionRow(
                flag = LocaleHelper.languageFlag(language.tag),
                label = language.nativeName,
                selected = selectedTag == language.tag,
                onClick = { onSelect(language.tag) },
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    flag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = flag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        ButtonLabel(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            floorFraction = SETTINGS_LABEL_MIN_SCALE,
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun PdfSettingSlider(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.PictureAsPdf,
                null,
                Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            ButtonLabel(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                // Four lines (#62): "Maximale Ausgaberesolution" next to the
                // value text ("3000 px") has the vertical room above the
                // slider, and the long word hyphenates ("Ausgabe-/
                // resolution") instead of showing "Ausgaberesoluti…".
                maxLines = 4,
                floorFraction = SETTINGS_LABEL_MIN_SCALE,
                hyphenate = true,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Preview(widthDp = 320, showBackground = true)
@Composable
private fun FontFamilyCardsLargeScalePreview() {
    // #62: the font labels used to truncate at font scale 2.0; in the 2×2
    // group they wrap at the space and share one size. #125: short names.
    MaterialTheme {
        Column(Modifier.padding(12.dp)) {
            FontFamilyCards(selected = PdfFont.SANS, onSelect = {})
        }
    }
}
