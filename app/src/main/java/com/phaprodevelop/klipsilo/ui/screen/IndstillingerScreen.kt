package com.phaprodevelop.klipsilo.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.settings.PdfFont
import com.phaprodevelop.klipsilo.data.settings.PdfSettingsStore
import com.phaprodevelop.klipsilo.data.settings.ThemeMode
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndstillingerScreen(
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
            Text(
                text = context.getString(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
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
                    Text(
                        text = context.getString(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            currentLanguage?.nativeName
                                ?: selectedLanguage
                                ?: context.getString(R.string.language_system),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Text(
                text = context.getString(R.string.display_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
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
                        Text(
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
                        )
                    }
                }
            }

            Spacer(Modifier.size(24.dp))

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
                Text(
                    text = context.getString(R.string.pdf_setting_font_family),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    PdfFont.SYSTEM to context.getString(R.string.pdf_font_system),
                    PdfFont.SANS to context.getString(R.string.pdf_font_sans),
                    PdfFont.SERIF to context.getString(R.string.pdf_font_serif),
                    PdfFont.MONO to context.getString(R.string.pdf_font_mono),
                ).forEach { (font, label) ->
                    val selected = pdfSettings.pdfFont == font
                    Card(
                        modifier =
                            Modifier.weight(1f).clickable {
                                pdfSettings = pdfSettings.copy(pdfFont = font)
                                PdfSettingsStore.save(context, pdfSettings)
                            },
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
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))

            PdfSettingSlider(
                label = context.getString(R.string.pdf_setting_margin_top_bottom),
                value = pdfSettings.marginTopBottomCm,
                valueText = String.format(Locale.US, "%.1f cm", pdfSettings.marginTopBottomCm),
                valueRange = 0.5f..4.0f,
                steps = 34,
                onValueChange = {
                    pdfSettings = pdfSettings.copy(marginTopBottomCm = it)
                    PdfSettingsStore.save(context, pdfSettings)
                },
            )
            PdfSettingSlider(
                label = context.getString(R.string.pdf_setting_margin_left_right),
                value = pdfSettings.marginLeftRightCm,
                valueText = String.format(Locale.US, "%.1f cm", pdfSettings.marginLeftRightCm),
                valueRange = 0.5f..4.0f,
                steps = 34,
                onValueChange = {
                    pdfSettings = pdfSettings.copy(marginLeftRightCm = it)
                    PdfSettingsStore.save(context, pdfSettings)
                },
            )
            PdfSettingSlider(
                label = context.getString(R.string.pdf_setting_font_size),
                value = pdfSettings.fontSizePt,
                valueText = String.format(Locale.US, "%.0f pt", pdfSettings.fontSizePt),
                valueRange = 8f..28f,
                steps = 19,
                onValueChange = {
                    pdfSettings = pdfSettings.copy(fontSizePt = it)
                    PdfSettingsStore.save(context, pdfSettings)
                },
            )
            PdfSettingSlider(
                label = context.getString(R.string.pdf_setting_line_spacing),
                value = pdfSettings.lineSpacing,
                valueText = String.format(Locale.US, "%.1f", pdfSettings.lineSpacing),
                valueRange = 1.0f..2.5f,
                steps = 14,
                onValueChange = {
                    pdfSettings = pdfSettings.copy(lineSpacing = it)
                    PdfSettingsStore.save(context, pdfSettings)
                },
            )
            PdfSettingSlider(
                label = context.getString(R.string.pdf_setting_max_output),
                value = pdfSettings.maxOutputPx.toFloat(),
                valueText = String.format(Locale.US, "%d px", pdfSettings.maxOutputPx),
                valueRange = 1000f..6000f,
                steps = 49,
                onValueChange = {
                    pdfSettings = pdfSettings.copy(maxOutputPx = it.toInt())
                    PdfSettingsStore.save(context, pdfSettings)
                },
            )

            Spacer(Modifier.size(24.dp))
            Text(
                text = context.getString(R.string.about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
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
                    Text(
                        text = context.getString(R.string.about),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
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
                    Text(
                        text = context.getString(R.string.privacy_policy),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
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
        Text(
            text = context.getString(R.string.language),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
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
